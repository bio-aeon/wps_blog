package su.wps.blog.services.mocks

import cats.Applicative
import su.wps.blog.models.domain.Language
import su.wps.blog.repositories.LanguageRepository

object LanguageRepositoryMock {
  def create[DB[_]](
    findActiveResult: List[Language] = Nil,
    findDefaultResult: Option[Language] = None,
    findByCodeResult: Option[Language] = None
  )(implicit DB: Applicative[DB]): LanguageRepository[DB] =
    new LanguageRepository[DB] {
      def findActive: DB[List[Language]] =
        DB.pure(findActiveResult)

      def findDefault: DB[Option[Language]] =
        DB.pure(findDefaultResult)

      def findByCode(code: String): DB[Option[Language]] =
        DB.pure(findByCodeResult)
    }
}
