package su.wps.blog.repositories.sql

import cats.data.NonEmptyList
import cats.syntax.applicative.*
import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import su.wps.blog.instances.time.*
import su.wps.blog.models.domain.Config
import tofu.doobie.LiftConnectionIO

final class ConfigSqlImpl private extends ConfigSql[ConnectionIO] {

  def findByName(name: String): ConnectionIO[Option[Config]] =
    sql"SELECT name, value, comment, created_at, id FROM configs WHERE name = $name"
      .query[Config]
      .option

  def findByNames(names: List[String]): ConnectionIO[List[Config]] =
    NonEmptyList.fromList(names) match {
      case Some(nel) =>
        (fr"SELECT name, value, comment, created_at, id FROM configs WHERE" ++
          Fragments.in(fr"name", nel))
          .query[Config]
          .to[List]
      case None =>
        List.empty[Config].pure[ConnectionIO]
    }
}

object ConfigSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): ConfigSql[DB] =
    new ConfigSqlImpl().mapK(L.liftF)
}
