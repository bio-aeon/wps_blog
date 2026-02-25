package su.wps.blog.services

import su.wps.blog.models.api.SkillCategoryResult

trait SkillService[F[_]] {
  def getSkillsByCategory: F[List[SkillCategoryResult]]
}
