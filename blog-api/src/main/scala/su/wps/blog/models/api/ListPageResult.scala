package su.wps.blog.models.api

import io.circe.Encoder

final case class ListPageResult(url: String, title: String, language: String)

object ListPageResult {
  implicit val encoder: Encoder[ListPageResult] =
    Encoder.forProduct3("url", "title", "language")(ListPageResult.unapply(_).get)
}
