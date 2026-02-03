package su.wps.blog.services

import su.wps.blog.models.api.HealthResponse

trait HealthService[F[_]] {
  def check: F[HealthResponse]
}
