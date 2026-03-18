package su.wps.blog.services

import su.wps.blog.models.api.LanguageResult

trait LanguageService[F[_]] {
  def getActiveLanguages: F[List[LanguageResult]]
  def resolveLanguage(explicit: Option[String], acceptHeader: Option[String]): F[String]
  def getDefaultLanguageCode: F[String]
}
