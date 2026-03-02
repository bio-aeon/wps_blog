package su.wps.blog.repositories.sql

import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import su.wps.blog.instances.time.*
import su.wps.blog.models.domain.{ContactSubmission, ContactSubmissionId}
import tofu.doobie.LiftConnectionIO

import java.time.ZonedDateTime

final class ContactSubmissionSqlImpl private extends ContactSubmissionSql[ConnectionIO] {

  def insert(submission: ContactSubmission): ConnectionIO[ContactSubmissionId] =
    sql"""INSERT INTO contact_submissions (name, email, subject, message, ip_address)
          VALUES (${submission.name}, ${submission.email}, ${submission.subject},
                  ${submission.message}, ${submission.ipAddress})""".update
      .withUniqueGeneratedKeys[ContactSubmissionId]("id")

  def countByIpSince(ip: String, since: ZonedDateTime): ConnectionIO[Int] =
    sql"""SELECT COUNT(*) FROM contact_submissions
          WHERE ip_address = $ip AND created_at >= $since"""
      .query[Int]
      .unique
}

object ContactSubmissionSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): ContactSubmissionSql[DB] =
    new ContactSubmissionSqlImpl().mapK(L.liftF)
}
