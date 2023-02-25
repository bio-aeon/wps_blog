package su.wps.blog.repositories.models

import cats.syntax.functor._
import doobie._
import su.wps.blog.tools.types._
import doobie.postgres.implicits._

import java.time.ZonedDateTime

final case class User(
  id: PosInt,
  username: Varchar[W.`255`.T],
  email: Varchar[W.`255`.T],
  password: Varchar[W.`255`.T],
  isActive: Boolean,
  isAdmin: Boolean,
  createdAt: ZonedDateTime
)

object User {

  object sql {
    def insert(user: User): ConnectionIO[Unit] = {
      val sql =
        """
          |insert into users (
          | id,
          | username,
          | email,
          | password,
          | is_active,
          | is_admin,
          | created_at
          |) values (?, ?, ?, ?, ?, ?, ?)
          |""".stripMargin
      Update[User](sql).run(user).void
    }
  }
}
