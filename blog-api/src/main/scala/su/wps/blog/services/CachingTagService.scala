package su.wps.blog.services

import cats.Monad
import su.wps.blog.models.api.{ListItemsResult, TagCloudResult, TagWithCountResult}

import scala.concurrent.duration.FiniteDuration

final class CachingTagService[F[_]: Monad] private (
  underlying: TagService[F],
  cache: CacheService[F],
  ttl: FiniteDuration
) extends TagService[F] {

  def getAllTags(lang: String): F[ListItemsResult[TagWithCountResult]] =
    cache.getOrLoad(s"tags:all:$lang", ttl)(underlying.getAllTags(lang))

  def getTagCloud(lang: String): F[TagCloudResult] =
    cache.getOrLoad(s"tags:cloud:$lang", ttl)(underlying.getTagCloud(lang))
}

object CachingTagService {
  def create[F[_]: Monad](
    underlying: TagService[F],
    cache: CacheService[F],
    ttl: FiniteDuration
  ): CachingTagService[F] =
    new CachingTagService(underlying, cache, ttl)
}
