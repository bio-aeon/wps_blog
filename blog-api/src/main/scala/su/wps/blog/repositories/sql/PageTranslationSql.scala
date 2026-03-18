package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.{PageId, PageTranslation}
import tofu.higherKind.derived.representableK

@derive(representableK)
trait PageTranslationSql[DB[_]] {
  def findByPageAndLanguage(pageId: PageId, lang: String): DB[Option[PageTranslation]]
  def findByPageId(pageId: PageId): DB[List[PageTranslation]]
  def findAvailableLanguages(pageId: PageId): DB[List[String]]
}
