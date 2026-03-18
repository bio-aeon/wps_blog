package su.wps.blog.models.api

import io.circe.Encoder

final case class SeoResult(
  title: Option[String],
  description: Option[String],
  keywords: Option[String]
)

object SeoResult {
  implicit val encoder: Encoder[SeoResult] =
    Encoder.forProduct3("title", "description", "keywords")(SeoResult.unapply(_).get)
}
