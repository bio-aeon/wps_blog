package su.wps.blog.repositories

import su.wps.blog.models.domain.{PageId, PageTranslation}

trait PageTranslationRepository[DB[_]] {
  def findByPageAndLanguage(pageId: PageId, lang: String): DB[Option[PageTranslation]]
  def findByPageId(pageId: PageId): DB[List[PageTranslation]]
  def findAvailableLanguages(pageId: PageId): DB[List[String]]
}
