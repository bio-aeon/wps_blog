package su.wps.blog.models.domain

import io.circe.Encoder

import java.time.{LocalDate, ZonedDateTime}

final case class Experience(
  company: String,
  position: String,
  description: String,
  startDate: LocalDate,
  endDate: Option[LocalDate],
  location: Option[String],
  companyUrl: Option[String],
  sortOrder: Int,
  isActive: Boolean,
  createdAt: ZonedDateTime,
  id: Option[ExperienceId] = None
) {
  def nonEmptyId: ExperienceId =
    id.getOrElse(throw new IllegalStateException("Empty experience id"))
}

final case class ExperienceId(value: Int) extends AnyVal

object ExperienceId {
  implicit val encoder: Encoder[ExperienceId] = Encoder[Int].contramap(_.value)
}
