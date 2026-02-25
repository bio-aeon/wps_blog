package su.wps.blog.models.domain

import java.time.ZonedDateTime

final case class ContactSubmission(
  name: String,
  email: String,
  subject: String,
  message: String,
  ipAddress: Option[String],
  isRead: Boolean,
  createdAt: ZonedDateTime,
  id: Option[ContactSubmissionId] = None
)

final case class ContactSubmissionId(value: Int) extends AnyVal
