package su.wps.blog.models

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
  created_at: ZonedDateTime,
  id: Option[PostId] = None
)

final case class PostId(value: Int) extends AnyVal
