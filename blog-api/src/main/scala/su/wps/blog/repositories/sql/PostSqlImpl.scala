package su.wps.blog.repositories.sql

import cats.effect.Sync
import cats.syntax.functor.*
import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import tofu.doobie.LiftConnectionIO
import su.wps.blog.models.domain.{Post, PostId}

final class PostSqlImpl private (implicit lh: LogHandler) extends PostSql[ConnectionIO] {
  val tableName: Fragment = Fragment.const("posts")

  def findAllWithLimitAndOffset(limit: Int, offset: Int): ConnectionIO[List[Post]] =
    (fr"SELECT name, short_text, text, author_id, views, meta_title, " ++
      fr"meta_keywords, meta_description, is_hidden, created_at, id FROM" ++ tableName ++
      fr"ORDER BY created_at DESC LIMIT $limit OFFSET $offset")
      .query[Post]
      .to[List]

  def findCount: ConnectionIO[Int] =
    (fr"SELECT COUNT(*) FROM" ++ tableName).query[Int].unique

  def findById(id: PostId): ConnectionIO[Option[Post]] =
    (fr"SELECT name, short_text, text, author_id, views, meta_title, " ++
      fr"meta_keywords, meta_description, is_hidden, created_at, id FROM" ++ tableName ++
      fr"WHERE id = $id")
      .query[Post]
      .option
}

object PostSqlImpl {
  def create[I[_]: Sync, DB[_]](implicit L: LiftConnectionIO[DB]): I[PostSql[DB]] =
    Slf4jDoobieLogHandler.create[I].map(implicit logger => new PostSqlImpl().mapK(L.liftF))
}
