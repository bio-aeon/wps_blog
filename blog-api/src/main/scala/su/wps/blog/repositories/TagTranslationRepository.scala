package su.wps.blog.repositories

import su.wps.blog.models.domain.{PostId, TagId}

trait TagTranslationRepository[DB[_]] {
  def findByTagIds(tagIds: List[TagId], lang: String): DB[Map[TagId, String]]
  def findAllTranslatedNames(lang: String): DB[Map[TagId, String]]
}
