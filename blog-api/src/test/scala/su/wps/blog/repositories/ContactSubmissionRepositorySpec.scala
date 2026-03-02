package su.wps.blog.repositories

import doobie.ConnectionIO
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.{ContactSubmission, ContactSubmissionId}
import su.wps.blog.tools.DbTest
import su.wps.blog.tools.syntax.*

import java.time.ZonedDateTime

class ContactSubmissionRepositorySpec extends Specification with DbTest {
  sequential

  lazy val repo: ContactSubmissionRepository[ConnectionIO] =
    ContactSubmissionRepositoryImpl.create[ConnectionIO]

  "ContactSubmissionRepository" >> {
    "insert" >> {
      "returns generated ID" >> {
        val submission = mkSubmission("Test", "test@example.com", "Subject", "Message body")

        val result = repo.insert(submission).runWithIO()

        result.value must beGreaterThan(0)
      }
    }

    "countByIpSince" >> {
      "counts submissions within time window" >> {
        val since = ZonedDateTime.now().minusHours(1)
        val submission1 = mkSubmission("A", "a@example.com", "Sub1", "Msg1", ip = "192.168.1.1")
        val submission2 = mkSubmission("B", "b@example.com", "Sub2", "Msg2", ip = "192.168.1.1")
        val submission3 = mkSubmission("C", "c@example.com", "Sub3", "Msg3", ip = "10.0.0.1")

        val test = for {
          _ <- repo.insert(submission1)
          _ <- repo.insert(submission2)
          _ <- repo.insert(submission3)
          count <- repo.countByIpSince("192.168.1.1", since)
        } yield count

        val result = test.runWithIO()
        result mustEqual 2
      }

      "returns 0 for IP with no submissions" >> {
        val since = ZonedDateTime.now().minusHours(1)

        val result = repo.countByIpSince("255.255.255.255", since).runWithIO()

        result mustEqual 0
      }
    }
  }

  private def mkSubmission(
    name: String,
    email: String,
    subject: String,
    message: String,
    ip: String = "127.0.0.1"
  ): ContactSubmission =
    ContactSubmission(
      name = name,
      email = email,
      subject = subject,
      message = message,
      ipAddress = Some(ip),
      isRead = false,
      createdAt = ZonedDateTime.now()
    )
}
