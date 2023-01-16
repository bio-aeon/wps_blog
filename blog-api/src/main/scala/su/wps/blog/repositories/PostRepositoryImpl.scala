package su.wps.blog.repositories

import cats.Functor
import cats.effect.Sync
import cats.syntax.functor._
import doobie._
import su.wps.blog.models.Post
import su.wps.blog.repositories.sql.{PostSql, PostSqlImpl}
import tofu.doobie.LiftConnectionIO

class PostRepositoryImpl[DB[_]](sql: PostSql[DB]) extends PostRepository[DB] {
  val tableName: Fragment = Fragment.const("auth_users")

  def findAllWithLimitAndOffset(limit: Int, offset: Int): DB[List[Post]] =
    sql.findAllWithLimitAndOffset(limit, offset)

}

object PostRepositoryImpl {

  def create[I[_]: Sync, DB[_]: LiftConnectionIO: Functor]: I[PostRepositoryImpl[DB]] =
    PostSqlImpl.create[I, DB].map { sql =>
      new PostRepositoryImpl[DB](sql)
    }
}
