package su.wps.blog.models.api

import io.circe.Encoder

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

final case class PageResult(
  id: Int,
  url: String,
  title: String,
  content: String,
  createdAt: ZonedDateTime
)

object PageResult {
  implicit val zonedDtEncoder: Encoder[ZonedDateTime] =
    Encoder[String].contramap(_.format(DateTimeFormatter.ISO_DATE_TIME))
  implicit val encoder: Encoder[PageResult] =
    Encoder.forProduct5("id", "url", "title", "content", "created_at")(PageResult.unapply(_).get)
}
