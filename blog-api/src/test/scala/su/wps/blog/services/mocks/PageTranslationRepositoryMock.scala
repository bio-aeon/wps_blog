package su.wps.blog.services.mocks

import cats.Applicative
import su.wps.blog.models.domain.{PageId, PageTranslation}
import su.wps.blog.repositories.PageTranslationRepository

object PageTranslationRepositoryMock {
  def create[DB[_]](
    findByPageAndLanguageResult: Option[PageTranslation] = None,
    findByPageIdResult: List[PageTranslation] = Nil,
    findAvailableLanguagesResult: List[String] = Nil
  )(implicit DB: Applicative[DB]): PageTranslationRepository[DB] =
    new PageTranslationRepository[DB] {
      def findByPageAndLanguage(pageId: PageId, lang: String): DB[Option[PageTranslation]] =
        DB.pure(findByPageAndLanguageResult)

      def findByPageId(pageId: PageId): DB[List[PageTranslation]] =
        DB.pure(findByPageIdResult)

      def findAvailableLanguages(pageId: PageId): DB[List[String]] =
        DB.pure(findAvailableLanguagesResult)
    }
}
