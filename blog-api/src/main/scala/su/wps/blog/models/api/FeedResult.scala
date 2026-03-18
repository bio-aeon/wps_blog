package su.wps.blog.models.api

import io.circe.Encoder
import su.wps.blog.models.domain.PostId

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

final case class FeedPostItem(
  id: PostId,
  name: String,
  shortText: Option[String],
  metaDescription: Option[String],
  createdAt: ZonedDateTime,
  language: String,
  tags: List[TagResult],
  availableLanguages: List[String]
)

object FeedPostItem {
  implicit val zonedDtEncoder: Encoder[ZonedDateTime] =
    Encoder[String].contramap(_.format(DateTimeFormatter.ISO_DATE_TIME))
  implicit val encoder: Encoder[FeedPostItem] =
    Encoder.forProduct8(
      "id",
      "name",
      "short_text",
      "meta_description",
      "created_at",
      "language",
      "tags",
      "available_languages"
    )(FeedPostItem.unapply(_).get)
}

final case class FeedPageItem(url: String, title: String, createdAt: ZonedDateTime)

object FeedPageItem {
  implicit val zonedDtEncoder: Encoder[ZonedDateTime] =
    Encoder[String].contramap(_.format(DateTimeFormatter.ISO_DATE_TIME))
  implicit val encoder: Encoder[FeedPageItem] =
    Encoder.forProduct3("url", "title", "created_at")(FeedPageItem.unapply(_).get)
}

final case class FeedTagItem(name: String, slug: String)

object FeedTagItem {
  implicit val encoder: Encoder[FeedTagItem] =
    Encoder.forProduct2("name", "slug")(FeedTagItem.unapply(_).get)
}

final case class FeedResult(
  posts: List[FeedPostItem],
  pages: List[FeedPageItem],
  tags: List[FeedTagItem]
)

object FeedResult {
  implicit val encoder: Encoder[FeedResult] =
    Encoder.forProduct3("posts", "pages", "tags")(FeedResult.unapply(_).get)
}
