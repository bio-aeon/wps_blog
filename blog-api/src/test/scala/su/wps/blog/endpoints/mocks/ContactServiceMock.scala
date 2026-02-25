package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.{ContactResponse, CreateContactRequest}
import su.wps.blog.services.ContactService

object ContactServiceMock {
  def create[F[_]: Applicative](
    result: ContactResponse = ContactResponse("Thank you for your message!")
  ): ContactService[F] = new ContactService[F] {
    def submitContact(request: CreateContactRequest, ip: String): F[ContactResponse] =
      result.pure[F]
  }
}
