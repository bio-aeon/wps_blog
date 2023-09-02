package su.wps.blog.models.api

import io.circe.Encoder
import io.circe.generic.extras._
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
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
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames
  implicit val zonedDtEncoder: Encoder[ZonedDateTime] =
    Encoder[String].contramap(_.format(DateTimeFormatter.ISO_DATE_TIME))
  implicit val encoder: Encoder[ListPostResult] = deriveConfiguredEncoder
}
