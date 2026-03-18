package su.wps.blog.repositories.sql

import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import su.wps.blog.instances.time.*
import su.wps.blog.models.domain.{PageId, PageTranslation}
import tofu.doobie.LiftConnectionIO

final class PageTranslationSqlImpl private extends PageTranslationSql[ConnectionIO] {

  private val selectFields: Fragment =
    fr"""page_id, language_code, title, content,
         seo_title, seo_description,
         translation_status, created_at, updated_at, id"""

  def findByPageAndLanguage(pageId: PageId, lang: String): ConnectionIO[Option[PageTranslation]] =
    (fr"SELECT" ++ selectFields ++
      fr"FROM page_translations WHERE page_id = ${pageId.value} AND language_code = $lang")
      .query[PageTranslation]
      .option

  def findByPageId(pageId: PageId): ConnectionIO[List[PageTranslation]] =
    (fr"SELECT" ++ selectFields ++
      fr"FROM page_translations WHERE page_id = ${pageId.value} ORDER BY language_code")
      .query[PageTranslation]
      .to[List]

  def findAvailableLanguages(pageId: PageId): ConnectionIO[List[String]] =
    sql"""SELECT language_code FROM page_translations
          WHERE page_id = ${pageId.value} AND translation_status = 'published'
          ORDER BY language_code"""
      .query[String]
      .to[List]
}

object PageTranslationSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): PageTranslationSql[DB] =
    new PageTranslationSqlImpl().mapK(L.liftF)
}
