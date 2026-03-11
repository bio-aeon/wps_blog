package su.wps.blog.services

import cats.Monad
import su.wps.blog.models.api.{ListItemsResult, TagCloudResult, TagWithCountResult}

import scala.concurrent.duration.FiniteDuration

final class CachingTagService[F[_]: Monad] private (
  underlying: TagService[F],
  cache: CacheService[F],
  ttl: FiniteDuration
) extends TagService[F] {

  def getAllTags: F[ListItemsResult[TagWithCountResult]] =
    cache.getOrLoad("tags:all", ttl)(underlying.getAllTags)

  def getTagCloud: F[TagCloudResult] =
    cache.getOrLoad("tags:cloud", ttl)(underlying.getTagCloud)
}

object CachingTagService {

  def create[F[_]: Monad](
    underlying: TagService[F],
    cache: CacheService[F],
    ttl: FiniteDuration
  ): CachingTagService[F] =
    new CachingTagService(underlying, cache, ttl)
}
