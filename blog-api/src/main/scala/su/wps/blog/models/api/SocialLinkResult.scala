package su.wps.blog.models.api

import io.circe.Encoder
import su.wps.blog.models.domain.SocialLinkId

final case class SocialLinkResult(
  id: SocialLinkId,
  platform: String,
  url: String,
  label: Option[String],
  icon: Option[String]
)

object SocialLinkResult {
  implicit val encoder: Encoder[SocialLinkResult] =
    Encoder.forProduct5("id", "platform", "url", "label", "icon")(r =>
      (r.id, r.platform, r.url, r.label, r.icon)
    )
}
