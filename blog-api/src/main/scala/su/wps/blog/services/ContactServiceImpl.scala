package su.wps.blog.services

import cats.Monad
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import mouse.anyf.*
import su.wps.blog.models.api.{ContactResponse, CreateContactRequest}
import su.wps.blog.models.domain.{AppErr, ContactSubmission}
import su.wps.blog.repositories.{ConfigRepository, ContactSubmissionRepository}
import tofu.Raise
import tofu.doobie.transactor.Txr

import java.time.ZonedDateTime

final class ContactServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  contactRepo: ContactSubmissionRepository[DB],
  configRepo: ConfigRepository[DB],
  xa: Txr[F, DB]
)(implicit R: Raise[F, AppErr])
    extends ContactService[F] {

  private val DefaultRateLimit = 5
  private val RateLimitWindowHours = 1
  private val SuccessMessage = "Thank you for your message! I'll get back to you soon."

  def submitContact(request: CreateContactRequest, ip: String): F[ContactResponse] = {
    if (request.website.exists(_.nonEmpty)) {
      return ContactResponse(SuccessMessage).pure[F]
    }

    val action: DB[(Int, Int)] = for {
      rateLimit <- configRepo
        .findByName("contact_rate_limit")
        .map(_.flatMap(c => scala.util.Try(c.value.toInt).toOption).getOrElse(DefaultRateLimit))
      since = ZonedDateTime.now().minusHours(RateLimitWindowHours)
      count <- contactRepo.countByIpSince(ip, since)
    } yield (rateLimit, count)

    action.thrushK(xa.trans).flatMap { case (limit, count) =>
      if (count >= limit) {
        R.raise(AppErr.ContactRateLimited(ip))
      } else {
        val submission = ContactSubmission(
          name = request.name,
          email = request.email,
          subject = request.subject,
          message = request.message,
          ipAddress = Some(ip),
          isRead = false,
          createdAt = ZonedDateTime.now()
        )
        contactRepo.insert(submission).thrushK(xa.trans).map(_ => ContactResponse(SuccessMessage))
      }
    }
  }
}

object ContactServiceImpl {
  def create[F[_]: Monad: Raise[*[_], AppErr], DB[_]: Monad](
    contactRepo: ContactSubmissionRepository[DB],
    configRepo: ConfigRepository[DB],
    xa: Txr[F, DB]
  ): ContactServiceImpl[F, DB] =
    new ContactServiceImpl(contactRepo, configRepo, xa)
}
