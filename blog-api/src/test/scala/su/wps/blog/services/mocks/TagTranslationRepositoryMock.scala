package su.wps.blog.services.mocks

import cats.Applicative
import su.wps.blog.models.domain.TagId
import su.wps.blog.repositories.TagTranslationRepository

object TagTranslationRepositoryMock {
  def create[DB[_]](
    findByTagIdsResult: Map[TagId, String] = Map.empty,
    findAllTranslatedNamesResult: Map[TagId, String] = Map.empty
  )(implicit DB: Applicative[DB]): TagTranslationRepository[DB] =
    new TagTranslationRepository[DB] {
      def findByTagIds(tagIds: List[TagId], lang: String): DB[Map[TagId, String]] =
        DB.pure(findByTagIdsResult)

      def findAllTranslatedNames(lang: String): DB[Map[TagId, String]] =
        DB.pure(findAllTranslatedNamesResult)
    }
}
