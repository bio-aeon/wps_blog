package su.wps.blog.models.api

import io.circe.{Decoder, Encoder}

final case class RateCommentRequest(isUpvote: Boolean)

object RateCommentRequest {
  implicit val decoder: Decoder[RateCommentRequest] =
    Decoder.forProduct1("is_upvote")(RateCommentRequest.apply)

  implicit val encoder: Encoder[RateCommentRequest] =
    Encoder.forProduct1("is_upvote")(_.isUpvote)
}
