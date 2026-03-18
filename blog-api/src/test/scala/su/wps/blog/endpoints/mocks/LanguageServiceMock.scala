package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.LanguageResult
import su.wps.blog.services.LanguageService

object LanguageServiceMock {
  def create[F[_]: Applicative](
    activeLanguagesResult: List[LanguageResult] = Nil,
    resolveLanguageResult: String = "en",
    defaultLanguageCodeResult: String = "en"
  ): LanguageService[F] =
    new LanguageService[F] {
      def getActiveLanguages: F[List[LanguageResult]] =
        activeLanguagesResult.pure[F]

      def resolveLanguage(explicit: Option[String], acceptHeader: Option[String]): F[String] =
        resolveLanguageResult.pure[F]

      def getDefaultLanguageCode: F[String] =
        defaultLanguageCodeResult.pure[F]
    }
}
