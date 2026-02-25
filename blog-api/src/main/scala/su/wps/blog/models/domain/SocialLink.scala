package su.wps.blog.models.domain

import io.circe.Encoder

import java.time.ZonedDateTime

final case class SocialLink(
  platform: String,
  url: String,
  label: Option[String],
  icon: Option[String],
  sortOrder: Int,
  isActive: Boolean,
  createdAt: ZonedDateTime,
  id: Option[SocialLinkId] = None
) {
  def nonEmptyId: SocialLinkId =
    id.getOrElse(throw new IllegalStateException("Empty social link id"))
}

final case class SocialLinkId(value: Int) extends AnyVal

object SocialLinkId {
  implicit val encoder: Encoder[SocialLinkId] = Encoder[Int].contramap(_.value)
}
