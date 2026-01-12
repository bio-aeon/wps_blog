package su.wps.blog.models.api

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*

final case class RateCommentRequest(isUpvote: Boolean)

object RateCommentRequest {
  implicit val decoder: Decoder[RateCommentRequest] = deriveDecoder[RateCommentRequest]
  implicit val encoder: Encoder[RateCommentRequest] = deriveEncoder[RateCommentRequest]
}
