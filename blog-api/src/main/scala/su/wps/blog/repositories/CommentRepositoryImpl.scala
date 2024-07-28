package su.wps.blog.repositories

import su.wps.blog.models.domain.{Comment, PostId}
import su.wps.blog.repositories.sql.{CommentSql, CommentSqlImpl}
import tofu.doobie.LiftConnectionIO

final class CommentRepositoryImpl[DB[_]] private (sql: CommentSql[DB])
    extends CommentRepository[DB] {
  def insert(comment: Comment): DB[Comment] =
    sql.insert(comment)

  def findCommentsByPostId(postId: PostId): DB[List[Comment]] =
    sql.findCommentsByPostId(postId)
}

object CommentRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: CommentRepositoryImpl[DB] = {
    val commentSql = CommentSqlImpl.create[DB]
    new CommentRepositoryImpl[DB](commentSql)
  }
}
