package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.TagId
import tofu.higherKind.derived.representableK

@derive(representableK)
trait TagTranslationSql[DB[_]] {
  def findByTagIds(tagIds: List[TagId], lang: String): DB[Map[TagId, String]]
  def findAllTranslatedNames(lang: String): DB[Map[TagId, String]]
}
