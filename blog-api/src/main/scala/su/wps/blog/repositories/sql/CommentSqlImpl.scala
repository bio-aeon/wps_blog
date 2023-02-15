package su.wps.blog.repositories.sql

import cats.effect.Sync
import cats.syntax.functor._
import cats.syntax.option._
import cats.tagless.syntax.functorK._
import doobie._
import doobie.implicits._
import doobie.implicits.javatimedrivernative._
import su.wps.blog.models.{Comment, CommentId, PostId}
import tofu.doobie.LiftConnectionIO

final class CommentSqlImpl private (implicit lh: LogHandler) extends CommentSql[ConnectionIO] {
  val tableName: Fragment = Fragment.const("comments")

  def insert(comment: Comment): ConnectionIO[Comment] =
    (fr"""
      insert into""" ++ tableName ++ fr"""(
        text,
        name,
        email,
        post_id,
        parent_id,
        rating,
        created_at
      )
      values (
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
    (fr"select text, name, email, post_id, left, right, " ++
      fr"tree_id, level, rating, created_at, parent_id, id from" ++ tableName ++
      fr"where post_id = $postId order by created_at desc")
      .query[Comment]
      .to[List]
}

object CommentSqlImpl {
  def create[I[_]: Sync, DB[_]](implicit L: LiftConnectionIO[DB]): I[CommentSql[DB]] =
    Slf4jDoobieLogHandler.create[I].map(implicit logger => new CommentSqlImpl().mapK(L.liftF))
}
