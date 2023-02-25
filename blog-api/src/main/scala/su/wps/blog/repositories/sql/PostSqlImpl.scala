package su.wps.blog.repositories.sql

import cats.effect.Sync
import cats.syntax.functor._
import cats.tagless.syntax.functorK._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import tofu.doobie.LiftConnectionIO
import su.wps.blog.models.{Post, PostId}

final class PostSqlImpl private (implicit lh: LogHandler) extends PostSql[ConnectionIO] {
  val tableName: Fragment = Fragment.const("posts")

  def findAllWithLimitAndOffset(limit: Int, offset: Int): ConnectionIO[List[Post]] =
    (fr"select name, short_text, text, author_id, views, meta_title, " ++
      fr"meta_keywords, meta_description, is_hidden, created_at, id from" ++ tableName ++
      fr"order by created_at desc limit $limit offset $offset")
      .query[Post]
      .to[List]

  def findCount: ConnectionIO[Int] =
    (fr"select count(*) from" ++ tableName).query[Int].unique

  def findById(id: PostId): ConnectionIO[Option[Post]] =
    (fr"select name, short_text, text, author_id, views, meta_title, " ++
      fr"meta_keywords, meta_description, is_hidden, created_at, id from" ++ tableName ++
      fr"where id = $id")
      .query[Post]
      .option
}

object PostSqlImpl {
  def create[I[_]: Sync, DB[_]](implicit L: LiftConnectionIO[DB]): I[PostSql[DB]] =
    Slf4jDoobieLogHandler.create[I].map(implicit logger => new PostSqlImpl().mapK(L.liftF))
}
