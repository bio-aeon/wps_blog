package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.Page
import tofu.higherKind.derived.representableK

@derive(representableK)
trait PageSql[DB[_]] {
  def findByUrl(url: String): DB[Option[Page]]
  def findAll: DB[List[Page]]
}
