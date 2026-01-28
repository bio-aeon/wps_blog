package su.wps.blog.models.api

import io.circe.Encoder
import io.circe.syntax.*

final case class ErrorResponse(
  code: String,
  message: String,
  details: Option[Map[String, String]] = None
)

object ErrorResponse {
  implicit val encoder: Encoder[ErrorResponse] =
    Encoder.forProduct3("code", "message", "details")(e => (e.code, e.message, e.details))

  def notFound(resource: String, id: String): ErrorResponse =
    ErrorResponse("NOT_FOUND", s"$resource not found: $id")

  def badRequest(message: String): ErrorResponse =
    ErrorResponse("BAD_REQUEST", message)

  def internal(message: String): ErrorResponse =
    ErrorResponse("INTERNAL_ERROR", message)

  def validationError(errors: Map[String, String]): ErrorResponse =
    ErrorResponse("VALIDATION_ERROR", "Request validation failed", Some(errors))
}
