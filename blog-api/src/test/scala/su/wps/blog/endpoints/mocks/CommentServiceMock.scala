package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.{CommentResult, CommentsListResult, CreateCommentRequest}
import su.wps.blog.models.domain.{CommentId, PostId}
import su.wps.blog.services.CommentService

import java.time.ZonedDateTime

object CommentServiceMock {
  def create[F[_]: Applicative](
    commentsForPostResult: CommentsListResult = CommentsListResult(Nil, 0),
    createCommentResult: Option[CommentResult] = None,
    rateCommentResult: Unit = (),
    deleteCommentResult: Unit = (),
    approveCommentResult: Unit = ()
  ): CommentService[F] =
    new CommentService[F] {
      def getCommentsForPost(postId: PostId): F[CommentsListResult] =
        commentsForPostResult.pure[F]

      def createComment(postId: PostId, request: CreateCommentRequest): F[CommentResult] =
        createCommentResult
          .getOrElse(
            CommentResult(
              id = CommentId(1),
              name = request.name,
              text = request.text,
              rating = 0,
              createdAt = ZonedDateTime.now(),
              replies = Nil
            )
          )
          .pure[F]

      def rateComment(commentId: CommentId, isUpvote: Boolean, ip: String): F[Unit] =
        rateCommentResult.pure[F]

      def deleteComment(commentId: CommentId): F[Unit] =
        deleteCommentResult.pure[F]

      def approveComment(commentId: CommentId): F[Unit] =
        approveCommentResult.pure[F]
    }
}
