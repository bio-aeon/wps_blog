package su.wps.blog.models.api

import io.circe.Encoder
import io.circe.syntax.*

final case class ErrorResponse(
  code: String,
  message: String,
  details: Option[Map[String, String]] = None
)

object ErrorResponse {
  val NotFoundCode = "NOT_FOUND"
  val BadRequestCode = "BAD_REQUEST"
  val InternalCode = "INTERNAL_ERROR"
  val ValidationCode = "VALIDATION_ERROR"
  val RateLimitedCode = "RATE_LIMITED"

  implicit val encoder: Encoder[ErrorResponse] =
    Encoder.forProduct3("code", "message", "details")(e => (e.code, e.message, e.details))

  def notFound(resource: String, id: String): ErrorResponse =
    ErrorResponse(NotFoundCode, s"$resource not found: $id")

  def badRequest(message: String): ErrorResponse =
    ErrorResponse(BadRequestCode, message)

  def internal(message: String): ErrorResponse =
    ErrorResponse(InternalCode, message)

  def validationError(errors: Map[String, String]): ErrorResponse =
    ErrorResponse(ValidationCode, "Request validation failed", Some(errors))

  def tooManyRequests(message: String): ErrorResponse =
    ErrorResponse(RateLimitedCode, message)
}
