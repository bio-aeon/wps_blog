package su.wps.blog.models.domain

import io.circe.Encoder

import java.time.ZonedDateTime

final case class Testimonial(
  authorName: String,
  authorTitle: Option[String],
  authorCompany: Option[String],
  authorUrl: Option[String],
  authorImageUrl: Option[String],
  quote: String,
  sortOrder: Int,
  isActive: Boolean,
  createdAt: ZonedDateTime,
  id: Option[TestimonialId] = None
) {
  def nonEmptyId: TestimonialId =
    id.getOrElse(throw new IllegalStateException("Empty testimonial id"))
}

final case class TestimonialId(value: Int) extends AnyVal

object TestimonialId {
  implicit val encoder: Encoder[TestimonialId] = Encoder[Int].contramap(_.value)
}
