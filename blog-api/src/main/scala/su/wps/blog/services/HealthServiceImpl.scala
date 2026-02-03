package su.wps.blog.services

import cats.Monad
import cats.syntax.functor.*
import su.wps.blog.models.api.HealthResponse

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

final class HealthServiceImpl[F[_]: Monad] private (
  dbCheck: F[Boolean]
) extends HealthService[F] {

  def check: F[HealthResponse] =
    dbCheck.map { isDbHealthy =>
      val dbStatus = if (isDbHealthy) "healthy" else "unhealthy"
      HealthResponse(
        status = if (isDbHealthy) "healthy" else "degraded",
        database = dbStatus,
        timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)
      )
    }
}

object HealthServiceImpl {
  def create[F[_]: Monad](dbCheck: F[Boolean]): HealthServiceImpl[F] =
    new HealthServiceImpl(dbCheck)
}
