package su.wps.blog.services

import su.wps.blog.models.api.{ContactResponse, CreateContactRequest}

trait ContactService[F[_]] {
  def submitContact(request: CreateContactRequest, ip: String): F[ContactResponse]
}
