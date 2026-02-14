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
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.CORS
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import su.wps.blog.config.{AppConfig, DbConfig, HttpServerConfig}
import su.wps.blog.endpoints.{ErrorHandler, RoutesImpl, SwaggerRoutes}
import su.wps.blog.repositories.{CommentRepositoryImpl, PageRepositoryImpl, PostRepositoryImpl, TagRepositoryImpl}
import su.wps.blog.repositories.sql.Slf4jDoobieLogHandler
import su.wps.blog.services.*
import tofu.doobie.transactor.Txr

object Program {

  def resource[F[_]: Network](implicit F: Async[F]): Resource[F, Unit] =
    for {
      logger <- Slf4jLogger.create[F].toResource
      _ <- logger.info("Starting application").toResource
      appResource: Resource[F, Unit] = for {
        appConfig <- parseAppConfig[F].toResource
        _ <- runMigrations[F](appConfig.db)
        xa <- mkTransactor[F](appConfig.db)
        postRepo = PostRepositoryImpl.create[xa.DB]
        tagRepo = TagRepositoryImpl.create[xa.DB]
        commentRepo = CommentRepositoryImpl.create[xa.DB]
        pageRepo = PageRepositoryImpl.create[xa.DB]
        postService = PostServiceImpl.create[F, xa.DB](postRepo, tagRepo, xa)
        commentService = CommentServiceImpl.create[F, xa.DB](commentRepo, xa)
        tagService = TagServiceImpl.create[F, xa.DB](tagRepo, xa)
        pageService = PageServiceImpl.create[F, xa.DB](pageRepo, xa)
        dbCheck = xa.trans(doobie.FC.isValid(1))
          .handleError(_ => false)
        healthService = HealthServiceImpl.create[F](dbCheck)
        routes = RoutesImpl.create[F](postService, commentService, tagService, pageService, healthService)
        routesWithErrorHandling = ErrorHandler(routes.routes)
        swaggerRoutes = SwaggerRoutes.routes[F]
        allRoutes = swaggerRoutes <+> routesWithErrorHandling
        _ <- mkHttpServer[F](appConfig.httpServer, allRoutes)
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
      ce <- ExecutionContexts.fixedThreadPool[F](32)
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
        .configure(ds => F.delay(ds.setAutoCommit(false)) *> F.delay(ds.setMaximumPoolSize(32)))
        .toResource
    } yield Txr.plain(tr)

  private def mkHttpServer[F[_]: Async: Network](
    serverConfig: HttpServerConfig,
    routes: HttpRoutes[F]
  ): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(serverConfig.interface)
      .withPort(serverConfig.port)
      .withHttpApp(CORS.policy.withAllowOriginAll(routes.orNotFound))
      .build
}
