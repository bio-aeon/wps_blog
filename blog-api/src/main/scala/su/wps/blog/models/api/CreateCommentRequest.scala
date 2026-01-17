package su.wps.blog.models.api

import io.circe.{Decoder, Encoder}

final case class CreateCommentRequest(
  name: String,
  email: String,
  text: String,
  parentId: Option[Int]
)

object CreateCommentRequest {
  implicit val decoder: Decoder[CreateCommentRequest] =
    Decoder.forProduct4("name", "email", "text", "parent_id")(CreateCommentRequest.apply)

  implicit val encoder: Encoder[CreateCommentRequest] =
    Encoder.forProduct4("name", "email", "text", "parent_id")(CreateCommentRequest.unapply(_).get)
}
