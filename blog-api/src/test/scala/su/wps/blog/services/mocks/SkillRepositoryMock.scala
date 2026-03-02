package su.wps.blog.services.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.domain.Skill
import su.wps.blog.repositories.SkillRepository

object SkillRepositoryMock {
  def create[DB[_]: Applicative](findAllActiveResult: List[Skill] = Nil): SkillRepository[DB] =
    new SkillRepository[DB] {
      def findAllActive: DB[List[Skill]] = findAllActiveResult.pure[DB]
    }
}
