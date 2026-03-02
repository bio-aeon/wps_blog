package su.wps.blog.repositories.models

import cats.syntax.functor.*
import doobie.*
import su.wps.blog.instances.time.*
import su.wps.blog.tools.types.*

import java.time.ZonedDateTime

final case class ContactSubmission(
  id: PosInt,
  name: Varchar[W.`255`.T],
  email: Varchar[W.`255`.T],
  subject: Varchar[W.`500`.T],
  message: Varchar[W.`1000`.T],
  ipAddress: Option[Varchar[W.`39`.T]],
  isRead: Boolean,
  createdAt: ZonedDateTime
)

object ContactSubmission {

  object sql {
    def insert(cs: ContactSubmission): ConnectionIO[Unit] = {
      val sql =
        """
          |INSERT INTO contact_submissions (
          | id,
          | name,
          | email,
          | subject,
          | message,
          | ip_address,
          | is_read,
          | created_at
          |) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
          |""".stripMargin
      Update[ContactSubmission](sql).run(cs).void
    }
  }
}
