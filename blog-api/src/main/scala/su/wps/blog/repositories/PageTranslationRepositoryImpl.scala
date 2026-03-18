package su.wps.blog.repositories

import su.wps.blog.models.domain.{PageId, PageTranslation}
import su.wps.blog.repositories.sql.{PageTranslationSql, PageTranslationSqlImpl}
import tofu.doobie.LiftConnectionIO

final class PageTranslationRepositoryImpl[DB[_]] private (sql: PageTranslationSql[DB])
    extends PageTranslationRepository[DB] {

  def findByPageAndLanguage(pageId: PageId, lang: String): DB[Option[PageTranslation]] =
    sql.findByPageAndLanguage(pageId, lang)

  def findByPageId(pageId: PageId): DB[List[PageTranslation]] =
    sql.findByPageId(pageId)

  def findAvailableLanguages(pageId: PageId): DB[List[String]] =
    sql.findAvailableLanguages(pageId)
}

object PageTranslationRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: PageTranslationRepositoryImpl[DB] = {
    val sql = PageTranslationSqlImpl.create[DB]
    new PageTranslationRepositoryImpl[DB](sql)
  }
}
