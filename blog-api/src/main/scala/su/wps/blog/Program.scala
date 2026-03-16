package su.wps.blog

import cats.ApplicativeError
import cats.effect.syntax.resource.*
import cats.effect.{Async, Resource}
import cats.syntax.applicativeError.*
import cats.syntax.apply.*
import cats.syntax.semigroupk.*
import com.typesafe.config.ConfigFactory
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import fly4s.*
import fly4s.data.*
import fs2.io.net.Network
import org.http4s.{HttpApp, HttpRoutes, Method}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.CORS
import org.typelevel.ci.CIString
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import su.wps.blog.config.*
import su.wps.blog.endpoints.*
import su.wps.blog.repositories.*
import su.wps.blog.repositories.sql.Slf4jDoobieLogHandler
import su.wps.blog.services.*
import tofu.doobie.transactor.Txr

import scala.concurrent.duration.*

object Program {

  def resource[F[_]: Network](implicit F: Async[F]): Resource[F, Unit] =
    for {
      logger <- Slf4jLogger.create[F].toResource
      _ <- logger.info("Starting application").toResource
      appResource: Resource[F, Unit] = for {
        appConfig <- parseAppConfig[F].toResource
        _ <- runMigrations[F](appConfig.db)
        xa <- mkTransactor[F](appConfig.db)
        cacheService = CacheServiceImpl.create[F](appConfig.cache.maxEntries)
        routes = {
          val postRepo = PostRepositoryImpl.create[xa.DB]
          val tagRepo = TagRepositoryImpl.create[xa.DB]
          val commentRepo = CommentRepositoryImpl.create[xa.DB]
          val pageRepo = PageRepositoryImpl.create[xa.DB]
          val skillRepo = SkillRepositoryImpl.create[xa.DB]
          val experienceRepo = ExperienceRepositoryImpl.create[xa.DB]
          val socialLinkRepo = SocialLinkRepositoryImpl.create[xa.DB]
          val contactRepo = ContactSubmissionRepositoryImpl.create[xa.DB]
          val configRepo = ConfigRepositoryImpl.create[xa.DB]
          val postService = PostServiceImpl.create[F, xa.DB](postRepo, tagRepo, xa)
          val commentService = CommentServiceImpl.create[F, xa.DB](commentRepo, xa)
          val tagService: TagService[F] = CachingTagService.create[F](
            TagServiceImpl.create[F, xa.DB](tagRepo, xa),
            cacheService,
            appConfig.cache.tagsTtlSeconds.seconds
          )
          val pageService = PageServiceImpl.create[F, xa.DB](pageRepo, xa)
          val skillService = SkillServiceImpl.create[F, xa.DB](skillRepo, xa)
          val experienceService = ExperienceServiceImpl.create[F, xa.DB](experienceRepo, xa)
          val socialLinkService = SocialLinkServiceImpl.create[F, xa.DB](socialLinkRepo, xa)
          val contactService =
            ContactServiceImpl.create[F, xa.DB](contactRepo, configRepo, xa)
          val feedService: FeedService[F] = CachingFeedService.create[F](
            FeedServiceImpl.create[F, xa.DB](postRepo, tagRepo, pageRepo, xa),
            cacheService,
            appConfig.cache.feedTtlSeconds.seconds
          )
          val aboutService: AboutService[F] = CachingAboutService.create[F](
            AboutServiceImpl.create[F, xa.DB](
              skillRepo,
              experienceRepo,
              socialLinkRepo,
              configRepo,
              pageRepo,
              xa
            ),
            cacheService,
            appConfig.cache.aboutTtlSeconds.seconds
          )
          val dbCheck = xa.trans(doobie.FC.isValid(1)).handleError(_ => false)
          val healthService = HealthServiceImpl.create[F](dbCheck)
          RoutesImpl.create[F](
            postService,
            commentService,
            tagService,
            pageService,
            healthService,
            skillService,
            experienceService,
            socialLinkService,
            contactService,
            aboutService,
            feedService
          )
        }
        routesWithErrorHandling = ErrorHandler(routes.routes)
        routesWithCaching = CacheMiddleware(routesWithErrorHandling)
        swaggerRoutes = SwaggerRoutes.routes[F]
        metricsRoutes = MetricsRoutes.routes[F]
        livenessRoutes = LivenessRoutes.routes[F]
        allRoutes = livenessRoutes <+> metricsRoutes <+> swaggerRoutes <+> routesWithCaching
        httpApp = mkHttpApp[F](appConfig, allRoutes)
        _ <- mkHttpServer[F](appConfig.httpServer, httpApp)
        _ <- Resource.make(F.unit)(_ => logger.info("Releasing application resources"))
      } yield ()
      _ <- appResource.onError { case err =>
        logger.error(err)(s"Failed to start application: ${err.getMessage}").toResource
      }
    } yield ()

  private def parseAppConfig[F[_]](implicit F: ApplicativeError[F, Throwable]): F[AppConfig] =
    F.catchNonFatal(ConfigSource.fromConfig(ConfigFactory.load()).loadOrThrow[AppConfig])

  private def runMigrations[F[_]](
    config: DbConfig
  )(implicit F: Async[F]): Resource[F, MigrateResult] =
    Fly4s
      .make[F](
        config.url,
        Some(config.username),
        Some(config.password.toCharArray),
        Fly4sConfig(defaultSchemaName = Some("public"))
      )
      .evalMap(_.migrate)

  private def mkTransactor[F[_]](
    config: DbConfig
  )(implicit F: Async[F]): Resource[F, Txr.Plain[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](config.pool.maximumPoolSize)
      tr <- HikariTransactor
        .newHikariTransactor[F](
          config.driver,
          config.url,
          config.username,
          config.password,
          ce,
          Some(Slf4jDoobieLogHandler.create[F])
        )
      _ <- tr
        .configure { ds =>
          F.delay {
            ds.setAutoCommit(false)
            ds.setMaximumPoolSize(config.pool.maximumPoolSize)
            ds.setMinimumIdle(config.pool.minimumIdle)
            ds.setIdleTimeout(config.pool.idleTimeoutMs)
            ds.setMaxLifetime(config.pool.maxLifetimeMs)
            ds.setConnectionTimeout(config.pool.connectionTimeoutMs)
            ds.setLeakDetectionThreshold(config.pool.leakDetectionThresholdMs)
          }
        }
        .toResource
    } yield Txr.plain(tr)

  @annotation.nowarn("msg=deprecated")
  private def gzipApp[F[_]: Async](app: HttpApp[F]): HttpApp[F] =
    org.http4s.server.middleware.GZip(app)

  private def mkHttpApp[F[_]: Async](
    appConfig: AppConfig,
    routes: HttpRoutes[F]
  ): HttpApp[F] = {
    val corsPolicy =
      if (appConfig.cors.allowedOrigins.isEmpty) {
        CORS.policy.withAllowOriginAll
      } else {
        val allowedSet = appConfig.cors.allowedOrigins.toSet
        CORS.policy
          .withAllowOriginHost { origin =>
            val base = s"${origin.scheme.value}://${origin.host.renderString}"
            val rendered = origin.port.fold(base)(p => s"$base:$p")
            allowedSet.contains(rendered)
          }
          .withAllowMethodsIn(Set(Method.GET, Method.POST, Method.OPTIONS))
          .withAllowHeadersIn(
            Set(
              CIString("Content-Type"),
              CIString("Accept"),
              CIString("X-Request-Id")
            )
          )
          .withMaxAge(3600.seconds)
      }

    val corsApp: HttpApp[F] = corsPolicy(routes.orNotFound)
    val applyRateLimit: HttpApp[F] => HttpApp[F] = RateLimitMiddleware[F](
      appConfig.rateLimit.maxRequests,
      appConfig.rateLimit.windowSeconds
    )
    val rateLimited: HttpApp[F] = applyRateLimit(corsApp)

    CorrelationIdMiddleware(
      SecurityHeadersMiddleware(
        MetricsMiddleware(gzipApp(rateLimited))
      )
    )
  }

  private def mkHttpServer[F[_]: Async: Network](
    serverConfig: HttpServerConfig,
    app: HttpApp[F]
  ): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(serverConfig.interface)
      .withPort(serverConfig.port)
      .withHttpApp(app)
      .build
}
