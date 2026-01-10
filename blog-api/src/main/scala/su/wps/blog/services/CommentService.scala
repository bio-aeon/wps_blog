package su.wps.blog.services

import su.wps.blog.models.api.{CommentResult, CommentsListResult, CreateCommentRequest}
import su.wps.blog.models.domain.PostId

trait CommentService[F[_]] {
  def getCommentsForPost(postId: PostId): F[CommentsListResult]

  def createComment(postId: PostId, request: CreateCommentRequest): F[CommentResult]
}
