package su.wps.blog.models.api

import io.circe.Encoder
import su.wps.blog.models.domain.PostId

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

final case class ListPostResult(
  id: PostId,
  name: String,
  shortText: String,
  createdAt: ZonedDateTime
)

object ListPostResult {
  implicit val zonedDtEncoder: Encoder[ZonedDateTime] =
    Encoder[String].contramap(_.format(DateTimeFormatter.ISO_DATE_TIME))
  implicit val encoder: Encoder[ListPostResult] =
    Encoder.forProduct4("id", "name", "short_text", "created_at")(ListPostResult.unapply(_).get)
}
