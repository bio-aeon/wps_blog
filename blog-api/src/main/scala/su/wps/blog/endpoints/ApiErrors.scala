package su.wps.blog.endpoints

import org.http4s.{InvalidMessageBodyFailure, MalformedMessageBodyFailure}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import su.wps.blog.models.api.ErrorResponse
import su.wps.blog.models.domain.AppErr

object ApiErrors {
  import TapirSupport.*

  private val UnexpectedMessage = "An unexpected error occurred"
  private val ContactRateLimitedMessage =
    "Too many contact submissions. Please try again later."

  def fromThrowable(error: Throwable): ErrorResponse = error match {
    case AppErr.PostNotFound(id) =>
      ErrorResponse.notFound("Post", id.value.toString)

    case AppErr.PageNotFound(url) =>
      ErrorResponse.notFound("Page", url)

    case AppErr.TranslationNotFound(entityType, id, language) =>
      ErrorResponse.notFound(s"$entityType translation", s"$id (lang: $language)")

    case AppErr.ValidationFailed(errors) =>
      ErrorResponse.validationError(errors)

    case AppErr.ContactRateLimited(_) =>
      ErrorResponse.tooManyRequests(ContactRateLimitedMessage)

    case e: InvalidMessageBodyFailure =>
      ErrorResponse.badRequest(e.getMessage)

    case e: MalformedMessageBodyFailure =>
      ErrorResponse.badRequest(e.getMessage)

    case _ =>
      ErrorResponse.internal(UnexpectedMessage)
  }

  def statusOf(error: ErrorResponse): StatusCode = error.code match {
    case ErrorResponse.NotFoundCode => StatusCode.NotFound
    case ErrorResponse.ValidationCode => StatusCode.BadRequest
    case ErrorResponse.BadRequestCode => StatusCode.BadRequest
    case ErrorResponse.RateLimitedCode => StatusCode.TooManyRequests
    case _ => StatusCode.InternalServerError
  }

  private def variant(code: String, status: StatusCode) =
    oneOfVariantValueMatcher(status, jsonBody[ErrorResponse]) { case e: ErrorResponse =>
      e.code == code
    }

  val output: EndpointOutput[ErrorResponse] =
    oneOf[ErrorResponse](
      variant(ErrorResponse.NotFoundCode, StatusCode.NotFound),
      variant(ErrorResponse.ValidationCode, StatusCode.BadRequest),
      variant(ErrorResponse.BadRequestCode, StatusCode.BadRequest),
      variant(ErrorResponse.RateLimitedCode, StatusCode.TooManyRequests),
      oneOfDefaultVariant(statusCode(StatusCode.InternalServerError).and(jsonBody[ErrorResponse]))
    )
}
