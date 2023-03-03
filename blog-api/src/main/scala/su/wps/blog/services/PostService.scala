package su.wps.blog.services

import su.wps.blog.models.api.{ListItemsResult, ListPostResult, PostResult}
import su.wps.blog.models.domain.PostId

trait PostService[F[_]] {
  def allPosts(limit: Int, offset: Int): F[ListItemsResult[ListPostResult]]

  def postById(id: PostId): F[PostResult]
}
