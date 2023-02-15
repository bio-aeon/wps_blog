package su.wps.blog.repositories

import cats.Functor
import cats.effect.Sync
import cats.syntax.functor._
import su.wps.blog.models.Post
import su.wps.blog.repositories.sql.{PostSql, PostSqlImpl}
import tofu.doobie.LiftConnectionIO

final class PostRepositoryImpl[DB[_]] private (sql: PostSql[DB]) extends PostRepository[DB] {
  def findAllWithLimitAndOffset(limit: Int, offset: Int): DB[List[Post]] =
    sql.findAllWithLimitAndOffset(limit, offset)

}

object PostRepositoryImpl {
  def create[I[_]: Sync, DB[_]: LiftConnectionIO: Functor]: I[PostRepositoryImpl[DB]] =
    PostSqlImpl.create[I, DB].map(new PostRepositoryImpl[DB](_))
}
