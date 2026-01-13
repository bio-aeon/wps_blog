package su.wps.blog.repositories.sql

import cats.syntax.option.*
import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import su.wps.blog.models.domain.{Comment, CommentId, PostId}
import su.wps.blog.instances.time.*
import tofu.doobie.LiftConnectionIO

final class CommentSqlImpl private extends CommentSql[ConnectionIO] {
  val tableName: Fragment = Fragment.const("comments")

  def insert(comment: Comment): ConnectionIO[Comment] =
    (fr"""
      INSERT INTO""" ++ tableName ++ fr"""(
        text,
        name,
        email,
        post_id,
        parent_id,
        rating,
        is_approved,
        created_at
      )
      VALUES (
        ${comment.text},
        ${comment.name},
        ${comment.email},
        ${comment.postId},
        ${comment.parentId},
        ${comment.rating},
        ${comment.isApproved},
        ${comment.createdAt}
      )
    """).update.withUniqueGeneratedKeys[CommentId]("id").map(id => comment.copy(id = id.some))

  def findById(commentId: CommentId): ConnectionIO[Option[Comment]] =
    (fr"SELECT text, name, email, post_id, rating, created_at, parent_id, is_approved, id FROM" ++ tableName ++
      fr"WHERE id = $commentId")
      .query[Comment]
      .option

  def findCommentsByPostId(postId: PostId): ConnectionIO[List[Comment]] =
    (fr"SELECT text, name, email, post_id, rating, created_at, parent_id, is_approved, id FROM" ++ tableName ++
      fr"WHERE post_id = $postId ORDER BY created_at ASC")
      .query[Comment]
      .to[List]

  def hasRated(commentId: CommentId, ip: String): ConnectionIO[Boolean] =
    sql"""
      SELECT EXISTS(
        SELECT 1 FROM comment_raters
        WHERE comment_id = ${commentId.value} AND ip = $ip
      )
    """.query[Boolean].unique

  def insertRater(commentId: CommentId, ip: String): ConnectionIO[Int] =
    sql"""
      INSERT INTO comment_raters (comment_id, ip)
      VALUES (${commentId.value}, $ip)
    """.update.run

  def updateRating(commentId: CommentId, delta: Int): ConnectionIO[Int] =
    sql"""
      UPDATE comments
      SET rating = rating + $delta
      WHERE id = ${commentId.value}
    """.update.run

  def delete(commentId: CommentId): ConnectionIO[Int] =
    sql"DELETE FROM comments WHERE id = ${commentId.value}".update.run

  def approve(commentId: CommentId): ConnectionIO[Int] =
    sql"UPDATE comments SET is_approved = true WHERE id = ${commentId.value}".update.run
}

object CommentSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): CommentSql[DB] =
    new CommentSqlImpl().mapK(L.liftF)
}
