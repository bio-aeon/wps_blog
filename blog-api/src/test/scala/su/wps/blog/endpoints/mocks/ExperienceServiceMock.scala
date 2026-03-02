package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.ExperienceResult
import su.wps.blog.services.ExperienceService

object ExperienceServiceMock {
  def create[F[_]: Applicative](result: List[ExperienceResult] = Nil): ExperienceService[F] =
    new ExperienceService[F] {
      def getExperiences: F[List[ExperienceResult]] = result.pure[F]
    }
}
