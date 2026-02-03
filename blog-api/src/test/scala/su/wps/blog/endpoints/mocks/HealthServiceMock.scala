package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.HealthResponse
import su.wps.blog.services.HealthService

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object HealthServiceMock {
  def create[F[_]: Applicative](
    status: String = "healthy",
    database: String = "healthy",
    timestamp: ZonedDateTime
  ): HealthService[F] = new HealthService[F] {
    def check: F[HealthResponse] =
      HealthResponse(
        status,
        database,
        timestamp.format(DateTimeFormatter.ISO_INSTANT)
      ).pure[F]
  }
}
