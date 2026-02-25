package su.wps.blog.models.api

import io.circe.Encoder
import su.wps.blog.models.domain.TestimonialId

final case class TestimonialResult(
  id: TestimonialId,
  authorName: String,
  authorTitle: Option[String],
  authorCompany: Option[String],
  authorUrl: Option[String],
  authorImageUrl: Option[String],
  quote: String
)

object TestimonialResult {
  implicit val encoder: Encoder[TestimonialResult] =
    Encoder.forProduct7(
      "id",
      "author_name",
      "author_title",
      "author_company",
      "author_url",
      "author_image_url",
      "quote"
    )(r =>
      (
        r.id,
        r.authorName,
        r.authorTitle,
        r.authorCompany,
        r.authorUrl,
        r.authorImageUrl,
        r.quote
      )
    )
}
