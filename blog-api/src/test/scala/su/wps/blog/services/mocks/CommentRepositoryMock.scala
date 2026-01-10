package su.wps.blog.services.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.domain.{Comment, CommentId, PostId}
import su.wps.blog.repositories.CommentRepository

object CommentRepositoryMock {
  def create[DB[_]: Applicative](
    findCommentsByPostIdResult: List[Comment] = Nil,
    insertResult: Comment => Comment = identity
  ): CommentRepository[DB] =
    new CommentRepository[DB] {
      def insert(comment: Comment): DB[Comment] =
        insertResult(comment).pure[DB]

      def findCommentsByPostId(postId: PostId): DB[List[Comment]] =
        findCommentsByPostIdResult.pure[DB]
    }

  def createWithAutoId[DB[_]: Applicative](
    findCommentsByPostIdResult: List[Comment] = Nil
  ): CommentRepository[DB] =
    create[DB](
      findCommentsByPostIdResult,
      insertResult = comment => comment.copy(id = Some(CommentId(1)))
    )
}
