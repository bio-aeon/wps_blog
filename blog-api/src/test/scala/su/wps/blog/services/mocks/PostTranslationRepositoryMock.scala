package su.wps.blog.services.mocks

import cats.Applicative
import su.wps.blog.models.domain.{PostId, PostTranslation}
import su.wps.blog.repositories.PostTranslationRepository

object PostTranslationRepositoryMock {
  def create[DB[_]](
    findByPostAndLanguageResult: Option[PostTranslation] = None,
    findByPostIdResult: List[PostTranslation] = Nil,
    findAvailableLanguagesResult: List[String] = Nil,
    findAvailableLanguagesByPostIdsResult: Map[PostId, List[String]] = Map.empty
  )(implicit DB: Applicative[DB]): PostTranslationRepository[DB] =
    new PostTranslationRepository[DB] {
      def findByPostAndLanguage(postId: PostId, lang: String): DB[Option[PostTranslation]] =
        DB.pure(findByPostAndLanguageResult)

      def findByPostId(postId: PostId): DB[List[PostTranslation]] =
        DB.pure(findByPostIdResult)

      def findAvailableLanguages(postId: PostId): DB[List[String]] =
        DB.pure(findAvailableLanguagesResult)

      def findAvailableLanguagesByPostIds(postIds: List[PostId]): DB[Map[PostId, List[String]]] =
        DB.pure(findAvailableLanguagesByPostIdsResult)
    }
}
