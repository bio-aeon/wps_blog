package su.wps.blog.services

import cats.Monad
import su.wps.blog.models.api.AboutResult

import scala.concurrent.duration.FiniteDuration

final class CachingAboutService[F[_]: Monad] private (
  underlying: AboutService[F],
  cache: CacheService[F],
  ttl: FiniteDuration
) extends AboutService[F] {

  def getAboutPage: F[AboutResult] =
    cache.getOrLoad("about:page", ttl)(underlying.getAboutPage)
}

object CachingAboutService {

  def create[F[_]: Monad](
    underlying: AboutService[F],
    cache: CacheService[F],
    ttl: FiniteDuration
  ): CachingAboutService[F] =
    new CachingAboutService(underlying, cache, ttl)
}
