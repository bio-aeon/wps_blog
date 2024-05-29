package su.wps.blog.models.api

import io.circe.Encoder
import io.circe.generic.extras.*
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

final case class PostResult(name: String, text: String, createdAt: ZonedDateTime)

object PostResult {
  implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames
  implicit val zonedDtEncoder: Encoder[ZonedDateTime] =
    Encoder[String].contramap(_.format(DateTimeFormatter.ISO_DATE_TIME))
  implicit val encoder: Encoder[PostResult] = deriveConfiguredEncoder
}
