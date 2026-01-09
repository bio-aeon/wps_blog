package su.wps.blog.services

import su.wps.blog.models.api.CommentsListResult
import su.wps.blog.models.domain.PostId

trait CommentService[F[_]] {
  def getCommentsForPost(postId: PostId): F[CommentsListResult]
}
