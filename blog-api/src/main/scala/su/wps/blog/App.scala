package su.wps.blog

import cats.effect.*
import cats.effect.syntax.all.*
import cats.syntax.applicativeError.*
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import distage.{DefaultModule, Injector, TagK}
import fs2.io.net.Network
import izumi.distage.framework.CoreCheckableAppSimple
import izumi.distage.model.definition.ModuleBase
import izumi.distage.model.plan.Roots
import org.http4s.server.Server
import org.typelevel.log4cats.slf4j.Slf4jLogger

object App extends IOApp {

  object checkable extends CoreCheckableAppSimple[IO] {
    override def module: ModuleBase = AppModule[IO]
    override def roots: Roots = Roots.target[Server]
  }

  override def run(args: List[String]): IO[ExitCode] =
    program[IO](checkable.module).as(ExitCode.Success)

  private def program[F[_]: Async: Network: TagK](module: ModuleBase): F[Unit] = {
    implicit val dm: DefaultModule[F] = DefaultModule.empty[F]
    for {
      logger <- Slf4jLogger.create[F]
      _ <- logger.info("Starting application")
      _ <- Injector[F]()
        .produceGet[Server](module)
        .use(_ => Async[F].never[Unit])
        .guaranteeCase(_ => logger.info("Releasing application resources"))
        .handleErrorWith { err =>
          logger.error(err)(s"Failed to start application: ${err.getMessage}") *> Async[F]
            .raiseError(err)
        }
    } yield ()
  }
}
