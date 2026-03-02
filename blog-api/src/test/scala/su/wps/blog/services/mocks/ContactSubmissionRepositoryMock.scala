package su.wps.blog.services.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.domain.{ContactSubmission, ContactSubmissionId}
import su.wps.blog.repositories.ContactSubmissionRepository

import java.time.ZonedDateTime

object ContactSubmissionRepositoryMock {
  def create[DB[_]: Applicative](
    insertResult: ContactSubmissionId = ContactSubmissionId(1),
    countByIpSinceResult: Int = 0
  ): ContactSubmissionRepository[DB] = new ContactSubmissionRepository[DB] {
    def insert(submission: ContactSubmission): DB[ContactSubmissionId] =
      insertResult.pure[DB]

    def countByIpSince(ip: String, since: ZonedDateTime): DB[Int] =
      countByIpSinceResult.pure[DB]
  }
}
