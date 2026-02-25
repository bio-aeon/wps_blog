package su.wps.blog.repositories

import su.wps.blog.models.domain.Skill

trait SkillRepository[DB[_]] {
  def findAllActive: DB[List[Skill]]
}
