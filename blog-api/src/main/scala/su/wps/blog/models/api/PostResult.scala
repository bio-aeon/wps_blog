package su.wps.blog.models.api

import io.circe.Encoder

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

final case class PostResult(name: String, text: String, createdAt: ZonedDateTime)

object PostResult {
  implicit val zonedDtEncoder: Encoder[ZonedDateTime] =
    Encoder[String].contramap(_.format(DateTimeFormatter.ISO_DATE_TIME))
  implicit val encoder: Encoder[PostResult] =
    Encoder.forProduct3("name", "text", "created_at")(PostResult.unapply(_).get)
}
