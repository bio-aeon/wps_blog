package su.wps.blog.services

import su.wps.blog.models.api.FeedResult

trait FeedService[F[_]] {
  def getFeed(lang: String): F[FeedResult]
}
