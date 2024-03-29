package su.wps.blog.models.domain

import io.circe.Encoder

import java.time.ZonedDateTime

final case class Post(
  name: String,
  shortText: String,
  text: String,
  authorId: UserId,
  views: Int,
  metaTitle: String,
  metaKeywords: String,
  metaDescription: String,
  isHidden: Boolean = true,
  createdAt: ZonedDateTime,
  id: Option[PostId] = None
) {
  def nonEmptyId: PostId = id.getOrElse(throw new IllegalStateException("Empty post id"))
}

final case class PostId(value: Int) extends AnyVal

object PostId {
  implicit val encoder: Encoder[PostId] = Encoder[Int].contramap(_.value)
}
