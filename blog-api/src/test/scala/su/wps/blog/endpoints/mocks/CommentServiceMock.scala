package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.{CommentResult, CommentsListResult}
import su.wps.blog.models.domain.PostId
import su.wps.blog.services.CommentService

object CommentServiceMock {
  def create[F[_]: Applicative](
    commentsForPostResult: CommentsListResult = CommentsListResult(Nil, 0)
  ): CommentService[F] =
    new CommentService[F] {
      def getCommentsForPost(postId: PostId): F[CommentsListResult] =
        commentsForPostResult.pure[F]
    }
}
