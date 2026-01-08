package su.wps.blog.services

import su.wps.blog.models.api.{ListItemsResult, ListPostResult, PostResult}
import su.wps.blog.models.domain.PostId

trait PostService[F[_]] {
  def allPosts(limit: Int, offset: Int): F[ListItemsResult[ListPostResult]]

  def postsByTag(tagSlug: String, limit: Int, offset: Int): F[ListItemsResult[ListPostResult]]

  def postById(id: PostId): F[PostResult]

  def incrementViewCount(id: PostId): F[Unit]

  def searchPosts(query: String, limit: Int, offset: Int): F[ListItemsResult[ListPostResult]]

  def recentPosts(count: Int): F[List[ListPostResult]]
}
