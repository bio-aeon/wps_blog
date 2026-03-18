package su.wps.blog.services

import su.wps.blog.models.api.{ListItemsResult, ListPostResult, PostResult}
import su.wps.blog.models.domain.PostId

trait PostService[F[_]] {
  def allPosts(lang: String, limit: Int, offset: Int): F[ListItemsResult[ListPostResult]]

  def postsByTag(
    lang: String,
    tagSlug: String,
    limit: Int,
    offset: Int
  ): F[ListItemsResult[ListPostResult]]

  def postById(lang: String, id: PostId): F[PostResult]

  def incrementViewCount(id: PostId): F[Unit]

  def searchPosts(
    lang: String,
    query: String,
    limit: Int,
    offset: Int
  ): F[ListItemsResult[ListPostResult]]

  def recentPosts(lang: String, count: Int): F[List[ListPostResult]]
}
