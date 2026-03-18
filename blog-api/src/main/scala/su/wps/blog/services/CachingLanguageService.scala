package su.wps.blog.services

import cats.Monad
import su.wps.blog.models.api.LanguageResult

import scala.concurrent.duration.FiniteDuration

final class CachingLanguageService[F[_]: Monad] private (
  underlying: LanguageService[F],
  cache: CacheService[F],
  ttl: FiniteDuration
) extends LanguageService[F] {

  def getActiveLanguages: F[List[LanguageResult]] =
    cache.getOrLoad("languages:active", ttl)(underlying.getActiveLanguages)

  def resolveLanguage(explicit: Option[String], acceptHeader: Option[String]): F[String] =
    underlying.resolveLanguage(explicit, acceptHeader)

  def getDefaultLanguageCode: F[String] =
    cache.getOrLoad("languages:default", ttl)(underlying.getDefaultLanguageCode)
}

object CachingLanguageService {
  def create[F[_]: Monad](
    underlying: LanguageService[F],
    cache: CacheService[F],
    ttl: FiniteDuration
  ): CachingLanguageService[F] =
    new CachingLanguageService(underlying, cache, ttl)
}
