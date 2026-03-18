package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.Language
import tofu.higherKind.derived.representableK

@derive(representableK)
trait LanguageSql[DB[_]] {
  def findActive: DB[List[Language]]
  def findDefault: DB[Option[Language]]
  def findByCode(code: String): DB[Option[Language]]
}
