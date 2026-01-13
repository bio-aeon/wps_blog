package su.wps.blog.services.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.domain.{Comment, CommentId, PostId}
import su.wps.blog.repositories.CommentRepository

object CommentRepositoryMock {
  def create[DB[_]: Applicative](
    findCommentsByPostIdResult: List[Comment] = Nil,
    findByIdResult: Option[Comment] = None,
    insertResult: Comment => Comment = identity,
    hasRatedResult: Boolean = false,
    insertRaterResult: Int = 1,
    updateRatingResult: Int = 1,
    deleteResult: Int = 1,
    approveResult: Int = 1
  ): CommentRepository[DB] =
    new CommentRepository[DB] {
      def insert(comment: Comment): DB[Comment] =
        insertResult(comment).pure[DB]

      def findById(commentId: CommentId): DB[Option[Comment]] =
        findByIdResult.pure[DB]

      def findCommentsByPostId(postId: PostId): DB[List[Comment]] =
        findCommentsByPostIdResult.pure[DB]

      def hasRated(commentId: CommentId, ip: String): DB[Boolean] =
        hasRatedResult.pure[DB]

      def insertRater(commentId: CommentId, ip: String): DB[Int] =
        insertRaterResult.pure[DB]

      def updateRating(commentId: CommentId, delta: Int): DB[Int] =
        updateRatingResult.pure[DB]

      def delete(commentId: CommentId): DB[Int] =
        deleteResult.pure[DB]

      def approve(commentId: CommentId): DB[Int] =
        approveResult.pure[DB]
    }

  def createWithAutoId[DB[_]: Applicative](
    findCommentsByPostIdResult: List[Comment] = Nil
  ): CommentRepository[DB] =
    create[DB](
      findCommentsByPostIdResult,
      insertResult = comment => comment.copy(id = Some(CommentId(1)))
    )
}
