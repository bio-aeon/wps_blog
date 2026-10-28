package su.wps.blog.telemetry

import cats.effect.Sync
import cats.syntax.functor.*
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.otel4s.sdk.common.Diagnostic

private[blog] object Slf4jDiagnostic {

  def create[F[_]: Sync]: F[Diagnostic[F]] =
    Slf4jLogger.create[F].map { logger =>
      new Diagnostic[F] {
        def info(message: => String): F[Unit] = logger.info(message)
        def error(message: => String): F[Unit] = logger.error(message)
        def error(message: => String, cause: Throwable): F[Unit] = logger.error(cause)(message)
      }
    }
}
