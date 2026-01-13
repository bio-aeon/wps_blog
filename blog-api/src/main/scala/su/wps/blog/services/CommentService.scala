package su.wps.blog.services

import su.wps.blog.models.api.{CommentResult, CommentsListResult, CreateCommentRequest}
import su.wps.blog.models.domain.{CommentId, PostId}

trait CommentService[F[_]] {
  def getCommentsForPost(postId: PostId): F[CommentsListResult]

  def createComment(postId: PostId, request: CreateCommentRequest): F[CommentResult]

  def rateComment(commentId: CommentId, isUpvote: Boolean, ip: String): F[Unit]

  def deleteComment(commentId: CommentId): F[Unit]

  def approveComment(commentId: CommentId): F[Unit]
}
