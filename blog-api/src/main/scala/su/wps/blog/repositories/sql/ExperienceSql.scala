package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.Experience
import tofu.higherKind.derived.representableK

@derive(representableK)
trait ExperienceSql[DB[_]] {
  def findAllActive: DB[List[Experience]]
}
