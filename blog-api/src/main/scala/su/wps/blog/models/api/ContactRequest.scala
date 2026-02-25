package su.wps.blog.models.api

import io.circe.{Decoder, Encoder}

final case class CreateContactRequest(
  name: String,
  email: String,
  subject: String,
  message: String,
  website: Option[String]
)

object CreateContactRequest {
  implicit val decoder: Decoder[CreateContactRequest] =
    Decoder.forProduct5("name", "email", "subject", "message", "website")(
      CreateContactRequest.apply
    )

  implicit val encoder: Encoder[CreateContactRequest] =
    Encoder.forProduct5("name", "email", "subject", "message", "website")(r =>
      (r.name, r.email, r.subject, r.message, r.website)
    )
}

final case class ContactResponse(message: String)

object ContactResponse {
  implicit val encoder: Encoder[ContactResponse] =
    Encoder.forProduct1("message")(_.message)
}
