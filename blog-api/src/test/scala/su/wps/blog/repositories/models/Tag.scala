package su.wps.blog.repositories.models

import cats.syntax.functor.*
import doobie.*
import su.wps.blog.tools.types.*

final case class Tag(id: PosInt, name: Varchar[W.`100`.T], slug: Varchar[W.`100`.T])

object Tag {

  object sql {
    def insert(tag: Tag): ConnectionIO[Unit] = {
      val sql =
        """
          |INSERT INTO tags (
          | id,
          | name,
          | slug
          |) VALUES (?, ?, ?)
          |""".stripMargin
      Update[Tag](sql).run(tag).void
    }
  }
}
