package su.wps.blog.repositories.models

import cats.syntax.functor.*
import doobie.*
import su.wps.blog.instances.time.*
import su.wps.blog.tools.types.*

import java.time.ZonedDateTime

final case class Page(
  id: PosInt,
  url: Varchar[W.`255`.T],
  title: Varchar[W.`255`.T],
  content: Varchar[W.`1000`.T],
  createdAt: ZonedDateTime
)

object Page {

  object sql {
    def insert(page: Page): ConnectionIO[Unit] = {
      val sql =
        """
          |INSERT INTO pages (
          | id,
          | url,
          | title,
          | content,
          | created_at
          |) VALUES (?, ?, ?, ?, ?)
          |""".stripMargin
      Update[Page](sql).run(page).void
    }
  }
}
