package su.wps.blog.services.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.domain.{Comment, PostId}
import su.wps.blog.repositories.CommentRepository

object CommentRepositoryMock {
  def create[DB[_]: Applicative](
    findCommentsByPostIdResult: List[Comment] = Nil
  ): CommentRepository[DB] =
    new CommentRepository[DB] {
      def findCommentsByPostId(postId: PostId): DB[List[Comment]] =
        findCommentsByPostIdResult.pure[DB]
    }
}
