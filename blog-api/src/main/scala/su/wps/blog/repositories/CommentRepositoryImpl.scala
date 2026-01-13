package su.wps.blog.repositories

import su.wps.blog.models.domain.{Comment, CommentId, PostId}
import su.wps.blog.repositories.sql.{CommentSql, CommentSqlImpl}
import tofu.doobie.LiftConnectionIO

final class CommentRepositoryImpl[DB[_]] private (sql: CommentSql[DB])
    extends CommentRepository[DB] {
  def insert(comment: Comment): DB[Comment] =
    sql.insert(comment)

  def findById(commentId: CommentId): DB[Option[Comment]] =
    sql.findById(commentId)

  def findCommentsByPostId(postId: PostId): DB[List[Comment]] =
    sql.findCommentsByPostId(postId)

  def hasRated(commentId: CommentId, ip: String): DB[Boolean] =
    sql.hasRated(commentId, ip)

  def insertRater(commentId: CommentId, ip: String): DB[Int] =
    sql.insertRater(commentId, ip)

  def updateRating(commentId: CommentId, delta: Int): DB[Int] =
    sql.updateRating(commentId, delta)

  def delete(commentId: CommentId): DB[Int] =
    sql.delete(commentId)

  def approve(commentId: CommentId): DB[Int] =
    sql.approve(commentId)
}

object CommentRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: CommentRepositoryImpl[DB] = {
    val commentSql = CommentSqlImpl.create[DB]
    new CommentRepositoryImpl[DB](commentSql)
  }
}
