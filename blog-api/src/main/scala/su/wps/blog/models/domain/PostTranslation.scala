package su.wps.blog.models.domain

import io.circe.Encoder

import java.time.ZonedDateTime

final case class PostTranslation(
  postId: PostId,
  languageCode: String,
  name: String,
  text: Option[String],
  shortText: Option[String],
  seoTitle: Option[String],
  seoDescription: Option[String],
  seoKeywords: Option[String],
  translationStatus: String,
  createdAt: ZonedDateTime,
  updatedAt: ZonedDateTime,
  id: Option[PostTranslationId] = None
)

final case class PostTranslationId(value: Int) extends AnyVal

object PostTranslationId {
  implicit val encoder: Encoder[PostTranslationId] = Encoder[Int].contramap(_.value)
}
