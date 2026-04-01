package su.wps.blog.repositories.models

import cats.syntax.functor.*
import doobie.*
import doobie.generic.auto.*
import su.wps.blog.instances.time.*
import su.wps.blog.tools.types.*

import java.time.{LocalDate, ZonedDateTime}

final case class Experience(
  id: PosInt,
  company: Varchar[W.`255`.T],
  position: Varchar[W.`255`.T],
  description: Varchar[W.`1000`.T],
  startDate: LocalDate,
  endDate: Option[LocalDate],
  location: Option[Varchar[W.`255`.T]],
  companyUrl: Option[Varchar[W.`500`.T]],
  sortOrder: Int,
  isActive: Boolean,
  createdAt: ZonedDateTime
)

object Experience {

  object sql {
    def insert(experience: Experience): ConnectionIO[Unit] = {
      val sql =
        """
          |INSERT INTO experiences (
          | id,
          | company,
          | position,
          | description,
          | start_date,
          | end_date,
          | location,
          | company_url,
          | sort_order,
          | is_active,
          | created_at
          |) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          |""".stripMargin
      Update[Experience](sql).run(experience).void
    }
  }
}
