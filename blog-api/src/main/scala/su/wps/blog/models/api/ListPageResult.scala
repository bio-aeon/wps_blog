package su.wps.blog.models.api

import io.circe.Encoder

final case class ListPageResult(
  url: String,
  title: String
)

object ListPageResult {
  implicit val encoder: Encoder[ListPageResult] =
    Encoder.forProduct2("url", "title")(ListPageResult.unapply(_).get)
}
