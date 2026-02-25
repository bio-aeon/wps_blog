package su.wps.blog.repositories

import su.wps.blog.models.domain.{ContactSubmission, ContactSubmissionId}
import su.wps.blog.repositories.sql.{ContactSubmissionSql, ContactSubmissionSqlImpl}
import tofu.doobie.LiftConnectionIO

import java.time.ZonedDateTime

final class ContactSubmissionRepositoryImpl[DB[_]] private (
  sql: ContactSubmissionSql[DB]
) extends ContactSubmissionRepository[DB] {
  def insert(submission: ContactSubmission): DB[ContactSubmissionId] =
    sql.insert(submission)

  def countByIpSince(ip: String, since: ZonedDateTime): DB[Int] =
    sql.countByIpSince(ip, since)
}

object ContactSubmissionRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: ContactSubmissionRepositoryImpl[DB] = {
    val contactSql = ContactSubmissionSqlImpl.create[DB]
    new ContactSubmissionRepositoryImpl[DB](contactSql)
  }
}
