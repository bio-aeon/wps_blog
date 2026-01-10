package su.wps.blog.models.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

final case class CreateCommentRequest(
  name: String,
  email: String,
  text: String,
  parentId: Option[Int]
)

object CreateCommentRequest {
  implicit val decoder: Decoder[CreateCommentRequest] = deriveDecoder[CreateCommentRequest]
  implicit val encoder: Encoder[CreateCommentRequest] = deriveEncoder[CreateCommentRequest]
}
