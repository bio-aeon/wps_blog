package su.wps.blog.services

import cats.Monad
import su.wps.blog.models.api.FeedResult

import scala.concurrent.duration.FiniteDuration

final class CachingFeedService[F[_]: Monad] private (
  underlying: FeedService[F],
  cache: CacheService[F],
  ttl: FiniteDuration
) extends FeedService[F] {

  def getFeed: F[FeedResult] =
    cache.getOrLoad("feed:all", ttl)(underlying.getFeed)
}

object CachingFeedService {

  def create[F[_]: Monad](
    underlying: FeedService[F],
    cache: CacheService[F],
    ttl: FiniteDuration
  ): CachingFeedService[F] =
    new CachingFeedService(underlying, cache, ttl)
}
