package su.wps.blog.models.api

import io.circe.{Encoder, Json}
import su.wps.blog.models.domain.CommentId

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

final case class CommentResult(
  id: CommentId,
  name: String,
  text: String,
  rating: Int,
  createdAt: ZonedDateTime,
  replies: List[CommentResult]
)

object CommentResult {
  implicit val zonedDtEncoder: Encoder[ZonedDateTime] =
    Encoder[String].contramap(_.format(DateTimeFormatter.ISO_DATE_TIME))

  implicit val encoder: Encoder[CommentResult] = new Encoder[CommentResult] {
    def apply(c: CommentResult): Json = Json.obj(
      "id" -> Json.fromInt(c.id.value),
      "name" -> Json.fromString(c.name),
      "text" -> Json.fromString(c.text),
      "rating" -> Json.fromInt(c.rating),
      "created_at" -> zonedDtEncoder(c.createdAt),
      "replies" -> Json.fromValues(c.replies.map(apply))
    )
  }
}

final case class CommentsListResult(comments: List[CommentResult], total: Int)

object CommentsListResult {
  implicit val encoder: Encoder[CommentsListResult] =
    Encoder.forProduct2("comments", "total")(c => (c.comments, c.total))
}
