package su.wps.blog

import cats.ApplicativeError
import cats.effect.{Async, Resource}
import cats.effect.syntax.all.*
import cats.syntax.applicativeError.*
import cats.syntax.semigroupk.*
import com.typesafe.config.ConfigFactory
import distage.{Lifecycle, ModuleDef, TagK}
import doobie.ConnectionIO
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import fly4s.*
import fly4s.data.*
import fs2.io.net.Network
import org.http4s.{HttpApp, HttpRoutes, Method}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.{CORS, GZip}
import org.typelevel.ci.CIString
import pureconfig.ConfigSource
import su.wps.blog.config.*
import su.wps.blog.endpoints.*
import su.wps.blog.repositories.*
import su.wps.blog.repositories.sql.Slf4jDoobieLogHandler
import su.wps.blog.services.*
import tofu.doobie.transactor.Txr

import scala.concurrent.duration.*

object AppModule {

  def apply[F[_]: Async: Network: TagK]: ModuleDef = new ModuleDef {
    make[AppConfig].fromEffect(parseAppConfig[F])
    make[DbConfig].from((_: AppConfig).db)
    make[HttpServerConfig].from((_: AppConfig).httpServer)
    make[CacheConfig].from((_: AppConfig).cache)
    make[CorsConfig].from((_: AppConfig).cors)
    make[RateLimitConfig].from((_: AppConfig).rateLimit)

    make[MigrateResult].fromResource { (c: DbConfig) =>
      Lifecycle.fromCats(runMigrations[F](c))
    }
    make[Txr.Plain[F]].fromResource { (c: DbConfig, _: MigrateResult) =>
      Lifecycle.fromCats(mkTransactor[F](c))
    }
    make[CacheService[F]].from { (c: CacheConfig) =>
      CacheServiceImpl.create[F](c.maxEntries)
    }

    make[PostRepository[ConnectionIO]].from(PostRepositoryImpl.create[ConnectionIO])
    make[TagRepository[ConnectionIO]].from(TagRepositoryImpl.create[ConnectionIO])
    make[CommentRepository[ConnectionIO]].from(CommentRepositoryImpl.create[ConnectionIO])
    make[PageRepository[ConnectionIO]].from(PageRepositoryImpl.create[ConnectionIO])
    make[SkillRepository[ConnectionIO]].from(SkillRepositoryImpl.create[ConnectionIO])
    make[ExperienceRepository[ConnectionIO]]
      .from(ExperienceRepositoryImpl.create[ConnectionIO])
    make[SocialLinkRepository[ConnectionIO]]
      .from(SocialLinkRepositoryImpl.create[ConnectionIO])
    make[ContactSubmissionRepository[ConnectionIO]]
      .from(ContactSubmissionRepositoryImpl.create[ConnectionIO])
    make[ConfigRepository[ConnectionIO]].from(ConfigRepositoryImpl.create[ConnectionIO])
    make[LanguageRepository[ConnectionIO]]
      .from(LanguageRepositoryImpl.create[ConnectionIO])
    make[PostTranslationRepository[ConnectionIO]]
      .from(PostTranslationRepositoryImpl.create[ConnectionIO])
    make[PageTranslationRepository[ConnectionIO]]
      .from(PageTranslationRepositoryImpl.create[ConnectionIO])
    make[TagTranslationRepository[ConnectionIO]]
      .from(TagTranslationRepositoryImpl.create[ConnectionIO])

    make[PostService[F]].from {
      (
        postRepo: PostRepository[ConnectionIO],
        tagRepo: TagRepository[ConnectionIO],
        ptRepo: PostTranslationRepository[ConnectionIO],
        ttRepo: TagTranslationRepository[ConnectionIO],
        xa: Txr.Plain[F]
      ) =>
        PostServiceImpl.create[F, ConnectionIO](postRepo, tagRepo, ptRepo, ttRepo, xa)
    }
    make[CommentService[F]].from { (r: CommentRepository[ConnectionIO], xa: Txr.Plain[F]) =>
      CommentServiceImpl.create[F, ConnectionIO](r, xa)
    }
    make[PageService[F]].from {
      (
        r: PageRepository[ConnectionIO],
        pt: PageTranslationRepository[ConnectionIO],
        xa: Txr.Plain[F]
      ) =>
        PageServiceImpl.create[F, ConnectionIO](r, pt, xa)
    }
    make[SkillService[F]].from { (r: SkillRepository[ConnectionIO], xa: Txr.Plain[F]) =>
      SkillServiceImpl.create[F, ConnectionIO](r, xa)
    }
    make[ExperienceService[F]].from { (r: ExperienceRepository[ConnectionIO], xa: Txr.Plain[F]) =>
      ExperienceServiceImpl.create[F, ConnectionIO](r, xa)
    }
    make[SocialLinkService[F]].from { (r: SocialLinkRepository[ConnectionIO], xa: Txr.Plain[F]) =>
      SocialLinkServiceImpl.create[F, ConnectionIO](r, xa)
    }
    make[ContactService[F]].from {
      (
        cr: ContactSubmissionRepository[ConnectionIO],
        cfgr: ConfigRepository[ConnectionIO],
        xa: Txr.Plain[F]
      ) =>
        ContactServiceImpl.create[F, ConnectionIO](cr, cfgr, xa)
    }
    make[HealthService[F]].from { (xa: Txr.Plain[F]) =>
      HealthServiceImpl.create[F](xa.trans(doobie.FC.isValid(1)).handleError(_ => false))
    }

    make[TagService[F]].from {
      (
        tagRepo: TagRepository[ConnectionIO],
        ttRepo: TagTranslationRepository[ConnectionIO],
        xa: Txr.Plain[F],
        cache: CacheService[F],
        cc: CacheConfig
      ) =>
        CachingTagService.create[F](
          TagServiceImpl.create[F, ConnectionIO](tagRepo, ttRepo, xa),
          cache,
          cc.tagsTtlSeconds.seconds
        )
    }
    make[FeedService[F]].from {
      (
        postRepo: PostRepository[ConnectionIO],
        tagRepo: TagRepository[ConnectionIO],
        pageRepo: PageRepository[ConnectionIO],
        ptRepo: PostTranslationRepository[ConnectionIO],
        xa: Txr.Plain[F],
        cache: CacheService[F],
        cc: CacheConfig
      ) =>
        CachingFeedService.create[F](
          FeedServiceImpl.create[F, ConnectionIO](postRepo, tagRepo, pageRepo, ptRepo, xa),
          cache,
          cc.feedTtlSeconds.seconds
        )
    }
    make[AboutService[F]].from {
      (
        skillRepo: SkillRepository[ConnectionIO],
        expRepo: ExperienceRepository[ConnectionIO],
        slRepo: SocialLinkRepository[ConnectionIO],
        cfgRepo: ConfigRepository[ConnectionIO],
        pageRepo: PageRepository[ConnectionIO],
        xa: Txr.Plain[F],
        cache: CacheService[F],
        cc: CacheConfig
      ) =>
        CachingAboutService.create[F](
          AboutServiceImpl
            .create[F, ConnectionIO](skillRepo, expRepo, slRepo, cfgRepo, pageRepo, xa),
          cache,
          cc.aboutTtlSeconds.seconds
        )
    }
    make[LanguageService[F]].from {
      (
        r: LanguageRepository[ConnectionIO],
        xa: Txr.Plain[F],
        cache: CacheService[F],
        cc: CacheConfig
      ) =>
        CachingLanguageService.create[F](
          LanguageServiceImpl.create[F, ConnectionIO](r, xa),
          cache,
          cc.tagsTtlSeconds.seconds
        )
    }

    make[RoutesImpl[F]].from {
      (
        ps: PostService[F],
        cs: CommentService[F],
        ts: TagService[F],
        pgs: PageService[F],
        hs: HealthService[F],
        sks: SkillService[F],
        es: ExperienceService[F],
        sls: SocialLinkService[F],
        cts: ContactService[F],
        as: AboutService[F],
        fs: FeedService[F],
        ls: LanguageService[F]
      ) =>
        RoutesImpl.create[F](ps, cs, ts, pgs, hs, sks, es, sls, cts, as, fs, ls)
    }
    make[HttpApp[F]].from { (routes: RoutesImpl[F], appConfig: AppConfig) =>
      val withMiddleware = CacheMiddleware(ErrorHandler(routes.routes))
      val allRoutes: HttpRoutes[F] =
        LivenessRoutes.routes[F] <+> MetricsRoutes.routes[F] <+>
          SwaggerRoutes.routes[F] <+> withMiddleware
      mkHttpApp[F](appConfig, allRoutes)
    }
    make[Server].fromResource { (c: HttpServerConfig, app: HttpApp[F]) =>
      Lifecycle.fromCats(mkHttpServer[F](c, app))
    }
  }

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
      _ <- tr.configure { ds =>
        F.delay {
          ds.setAutoCommit(false)
          ds.setMaximumPoolSize(config.pool.maximumPoolSize)
          ds.setMinimumIdle(config.pool.minimumIdle)
          ds.setIdleTimeout(config.pool.idleTimeoutMs)
          ds.setMaxLifetime(config.pool.maxLifetimeMs)
          ds.setConnectionTimeout(config.pool.connectionTimeoutMs)
          ds.setLeakDetectionThreshold(config.pool.leakDetectionThresholdMs)
        }
      }.toResource
    } yield Txr.plain(tr)

  @annotation.nowarn("msg=deprecated")
  private def gzipApp[F[_]: Async](app: HttpApp[F]): HttpApp[F] =
    GZip(app)

  private def mkHttpApp[F[_]: Async](appConfig: AppConfig, routes: HttpRoutes[F]): HttpApp[F] = {
    val corsPolicy =
      if (appConfig.cors.allowedOrigins.isEmpty) {
        CORS.policy.withAllowOriginAll
      } else {
        val allowedSet = appConfig.cors.allowedOrigins.toSet
        CORS.policy
          .withAllowOriginHost { origin =>
            val base =
              s"${origin.scheme.value}://${origin.host.renderString}"
            val rendered = origin.port.fold(base)(p => s"$base:$p")
            allowedSet.contains(rendered)
          }
          .withAllowMethodsIn(Set(Method.GET, Method.POST, Method.OPTIONS))
          .withAllowHeadersIn(
            Set(CIString("Content-Type"), CIString("Accept"), CIString("X-Request-Id"))
          )
          .withMaxAge(3600.seconds)
      }

    val corsApp: HttpApp[F] = corsPolicy(routes.orNotFound)
    val applyRateLimit: HttpApp[F] => HttpApp[F] =
      RateLimitMiddleware[F](appConfig.rateLimit.maxRequests, appConfig.rateLimit.windowSeconds)
    val rateLimited: HttpApp[F] = applyRateLimit(corsApp)

    CorrelationIdMiddleware(SecurityHeadersMiddleware(MetricsMiddleware(gzipApp(rateLimited))))
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
