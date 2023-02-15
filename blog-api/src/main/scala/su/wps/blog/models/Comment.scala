package su.wps.blog.models

import java.time.ZonedDateTime

final case class Comment(
  text: String,
  name: String,
  email: String,
  postId: Int,
  rating: Int,
  createdAt: ZonedDateTime,
  parentId: Option[Int] = None,
  id: Option[CommentId] = None
)

final case class CommentId(value: Int) extends AnyVal
