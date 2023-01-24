package su.wps.blog.repositories.sql

import cats.effect.Sync
import cats.syntax.functor._
import cats.tagless.syntax.functorK._
import doobie._
import doobie.implicits._
import doobie.implicits.javatimedrivernative._
import su.wps.blog.models.{Comment, PostId}
import tofu.doobie.LiftConnectionIO

final class CommentSqlImpl private (implicit lh: LogHandler) extends CommentSql[ConnectionIO] {
  val tableName: Fragment = Fragment.const("comments")

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
