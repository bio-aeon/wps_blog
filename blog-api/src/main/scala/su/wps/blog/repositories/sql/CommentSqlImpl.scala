package su.wps.blog.repositories.sql

import cats.effect.Sync
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import su.wps.blog.models.domain.{Comment, CommentId, PostId}
import tofu.doobie.LiftConnectionIO

final class CommentSqlImpl private (implicit lh: LogHandler) extends CommentSql[ConnectionIO] {
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
    (fr"SELECT text, name, email, post_id, left, right, " ++
      fr"tree_id, level, rating, created_at, parent_id, id FROM" ++ tableName ++
      fr"WHERE post_id = $postId ORDER BY created_at DESC")
      .query[Comment]
      .to[List]
}

object CommentSqlImpl {
  def create[I[_]: Sync, DB[_]](implicit L: LiftConnectionIO[DB]): I[CommentSql[DB]] =
    Slf4jDoobieLogHandler.create[I].map(implicit logger => new CommentSqlImpl().mapK(L.liftF))
}
