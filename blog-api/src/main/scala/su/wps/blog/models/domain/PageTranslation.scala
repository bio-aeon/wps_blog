package su.wps.blog.models.domain

import io.circe.Encoder

import java.time.ZonedDateTime

final case class PageTranslation(
  pageId: PageId,
  languageCode: String,
  title: String,
  content: Option[String],
  seoTitle: Option[String],
  seoDescription: Option[String],
  translationStatus: String,
  createdAt: ZonedDateTime,
  updatedAt: ZonedDateTime,
  id: Option[PageTranslationId] = None
)

final case class PageTranslationId(value: Int) extends AnyVal

object PageTranslationId {
  implicit val encoder: Encoder[PageTranslationId] = Encoder[Int].contramap(_.value)
}
