package su.wps.blog.repositories

import su.wps.blog.models.domain.{ContactSubmission, ContactSubmissionId}

import java.time.ZonedDateTime

trait ContactSubmissionRepository[DB[_]] {
  def insert(submission: ContactSubmission): DB[ContactSubmissionId]
  def countByIpSince(ip: String, since: ZonedDateTime): DB[Int]
}
