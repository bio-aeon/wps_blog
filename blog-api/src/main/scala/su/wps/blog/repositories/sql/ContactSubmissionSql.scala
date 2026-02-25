package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.{ContactSubmission, ContactSubmissionId}
import tofu.higherKind.derived.representableK

import java.time.ZonedDateTime

@derive(representableK)
trait ContactSubmissionSql[DB[_]] {
  def insert(submission: ContactSubmission): DB[ContactSubmissionId]
  def countByIpSince(ip: String, since: ZonedDateTime): DB[Int]
}
