package su.wps.blog.models.domain

import io.circe.Encoder

import java.time.ZonedDateTime

final case class TagTranslation(
  tagId: TagId,
  languageCode: String,
  name: String,
  createdAt: ZonedDateTime,
  id: Option[TagTranslationId] = None
)

final case class TagTranslationId(value: Int) extends AnyVal

object TagTranslationId {
  implicit val encoder: Encoder[TagTranslationId] = Encoder[Int].contramap(_.value)
}
