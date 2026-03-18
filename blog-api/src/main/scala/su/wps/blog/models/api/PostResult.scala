package su.wps.blog.models.api

import io.circe.Encoder
import su.wps.blog.models.domain.PostId

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

final case class PostResult(
  id: PostId,
  name: String,
  text: String,
  createdAt: ZonedDateTime,
  language: String,
  tags: List[TagResult],
  seo: Option[SeoResult],
  availableLanguages: List[String]
)

object PostResult {
  implicit val zonedDtEncoder: Encoder[ZonedDateTime] =
    Encoder[String].contramap(_.format(DateTimeFormatter.ISO_DATE_TIME))
  implicit val encoder: Encoder[PostResult] =
    Encoder.forProduct8(
      "id",
      "name",
      "text",
      "created_at",
      "language",
      "tags",
      "seo",
      "available_languages"
    )(PostResult.unapply(_).get)
}
