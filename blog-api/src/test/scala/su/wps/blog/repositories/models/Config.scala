package su.wps.blog.repositories.models

import cats.syntax.functor.*
import doobie.*
import su.wps.blog.instances.time.*
import su.wps.blog.tools.types.*

import java.time.ZonedDateTime

final case class Config(
  id: PosInt,
  name: Varchar[W.`255`.T],
  value: Varchar[W.`1000`.T],
  comment: Varchar[W.`255`.T],
  createdAt: ZonedDateTime
)

object Config {

  object sql {
    def insert(config: Config): ConnectionIO[Unit] = {
      val sql =
        """
          |INSERT INTO configs (
          | id,
          | name,
          | value,
          | comment,
          | created_at
          |) VALUES (?, ?, ?, ?, ?)
          |""".stripMargin
      Update[Config](sql).run(config).void
    }
  }
}
