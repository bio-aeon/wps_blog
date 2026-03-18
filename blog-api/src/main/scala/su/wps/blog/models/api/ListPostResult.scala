package su.wps.blog.models.api

import io.circe.Encoder
import su.wps.blog.models.domain.PostId

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

final case class ListPostResult(
  id: PostId,
  name: String,
  shortText: Option[String],
  createdAt: ZonedDateTime,
  language: String,
  tags: List[TagResult],
  availableLanguages: List[String]
)

object ListPostResult {
  implicit val zonedDtEncoder: Encoder[ZonedDateTime] =
    Encoder[String].contramap(_.format(DateTimeFormatter.ISO_DATE_TIME))
  implicit val encoder: Encoder[ListPostResult] =
    Encoder.forProduct7(
      "id",
      "name",
      "short_text",
      "created_at",
      "language",
      "tags",
      "available_languages"
    )(ListPostResult.unapply(_).get)
}
