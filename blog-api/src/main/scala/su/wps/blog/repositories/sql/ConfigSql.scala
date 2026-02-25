package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.Config
import tofu.higherKind.derived.representableK

@derive(representableK)
trait ConfigSql[DB[_]] {
  def findByName(name: String): DB[Option[Config]]
  def findByNames(names: List[String]): DB[List[Config]]
}
