package su.wps.blog.services.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.domain.Experience
import su.wps.blog.repositories.ExperienceRepository

object ExperienceRepositoryMock {
  def create[DB[_]: Applicative](
    findAllActiveResult: List[Experience] = Nil
  ): ExperienceRepository[DB] = new ExperienceRepository[DB] {
    def findAllActive: DB[List[Experience]] = findAllActiveResult.pure[DB]
  }
}
