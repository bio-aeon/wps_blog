package su.wps.blog.services

import cats.Id
import org.specs2.mutable.Specification
import su.wps.blog.models.api.{ContactResponse, CreateContactRequest}
import su.wps.blog.models.domain.{AppErr, Config, ConfigId, ContactSubmissionId}
import su.wps.blog.services.mocks.{ConfigRepositoryMock, ContactSubmissionRepositoryMock, TxrMock}
import tofu.Raise
import tofu.doobie.transactor.Txr

import java.time.ZonedDateTime

class ContactServiceSpec extends Specification {
  type RunF[A] = Either[Throwable, A]

  val xa: Txr[RunF, Id] = TxrMock.create[RunF]

  "ContactService.submitContact" >> {
    "returns success for valid submission" >> {
      val service = mkService()
      val request = mkRequest()

      service.submitContact(request, "127.0.0.1") must beRight.which { (r: ContactResponse) =>
        r.message.nonEmpty
      }
    }

    "returns silent success when honeypot field is filled" >> {
      val service = mkService()
      val request = mkRequest(website = Some("spam-bot-value"))

      service.submitContact(request, "127.0.0.1") must beRight.which { (r: ContactResponse) =>
        r.message.nonEmpty
      }
    }

    "raises ContactRateLimited when rate limit is exceeded" >> {
      val service = mkService(countByIpSinceResult = 5)
      val request = mkRequest()

      service.submitContact(request, "192.168.1.1") must beLeft.which {
        case AppErr.ContactRateLimited(ip) => ip == "192.168.1.1"
        case _ => false
      }
    }

    "uses config rate limit when available" >> {
      val rateConfig = Config("contact_rate_limit", "2", "", ZonedDateTime.now(), Some(ConfigId(1)))
      val service = mkService(findByNameResult = Some(rateConfig), countByIpSinceResult = 2)
      val request = mkRequest()

      service.submitContact(request, "10.0.0.1") must beLeft.which {
        case _: AppErr.ContactRateLimited => true
        case _ => false
      }
    }

    "allows submission when count is below limit" >> {
      val service = mkService(countByIpSinceResult = 4)
      val request = mkRequest()

      service.submitContact(request, "127.0.0.1") must beRight
    }
  }

  private def mkRequest(
    name: String = "John",
    email: String = "john@example.com",
    subject: String = "Hello",
    message: String = "Test message",
    website: Option[String] = None
  ): CreateContactRequest =
    CreateContactRequest(name, email, subject, message, website)

  private def mkService(
    insertResult: ContactSubmissionId = ContactSubmissionId(1),
    countByIpSinceResult: Int = 0,
    findByNameResult: Option[Config] = None
  ): ContactService[RunF] = {
    val contactRepo = ContactSubmissionRepositoryMock
      .create[Id](insertResult = insertResult, countByIpSinceResult = countByIpSinceResult)
    val configRepo = ConfigRepositoryMock.create[Id](findByNameResult = findByNameResult)
    ContactServiceImpl.create[RunF, Id](contactRepo, configRepo, xa)
  }
}
