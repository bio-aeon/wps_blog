package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.Skill
import tofu.higherKind.derived.representableK

@derive(representableK)
trait SkillSql[DB[_]] {
  def findAllActive: DB[List[Skill]]
}
