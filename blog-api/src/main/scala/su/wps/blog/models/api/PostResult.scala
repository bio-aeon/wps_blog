package su.wps.blog.models.api

import io.circe.Encoder

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

final case class PostResult(
  name: String,
  text: String,
  createdAt: ZonedDateTime,
  tags: List[TagResult]
)

object PostResult {
  implicit val zonedDtEncoder: Encoder[ZonedDateTime] =
    Encoder[String].contramap(_.format(DateTimeFormatter.ISO_DATE_TIME))
  implicit val encoder: Encoder[PostResult] =
    Encoder.forProduct4("name", "text", "created_at", "tags")(PostResult.unapply(_).get)
}
