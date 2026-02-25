package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.SkillCategoryResult
import su.wps.blog.services.SkillService

object SkillServiceMock {
  def create[F[_]: Applicative](
    result: List[SkillCategoryResult] = Nil
  ): SkillService[F] = new SkillService[F] {
    def getSkillsByCategory: F[List[SkillCategoryResult]] = result.pure[F]
  }
}
