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
        created_at
      )
      VALUES (
        ${comment.text},
        ${comment.name},
        ${comment.email},
        ${comment.postId},
        ${comment.parentId},
        ${comment.rating},
        ${comment.createdAt}
      )
    """).update.withUniqueGeneratedKeys[CommentId]("id").map(id => comment.copy(id = id.some))

  def findCommentsByPostId(postId: PostId): ConnectionIO[List[Comment]] =
    (fr"SELECT text, name, email, post_id, rating, created_at, parent_id, id FROM" ++ tableName ++
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
}

object CommentSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): CommentSql[DB] =
    new CommentSqlImpl().mapK(L.liftF)
}
