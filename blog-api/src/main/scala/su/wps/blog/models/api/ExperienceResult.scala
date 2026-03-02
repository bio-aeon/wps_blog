package su.wps.blog.models.api

import io.circe.Encoder
import su.wps.blog.models.domain.ExperienceId

import java.time.LocalDate
import java.time.format.DateTimeFormatter

final case class ExperienceResult(
  id: ExperienceId,
  company: String,
  position: String,
  description: String,
  startDate: LocalDate,
  endDate: Option[LocalDate],
  location: Option[String],
  companyUrl: Option[String]
)

object ExperienceResult {
  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  implicit val localDateEncoder: Encoder[LocalDate] =
    Encoder[String].contramap(_.format(dateFormatter))

  implicit val encoder: Encoder[ExperienceResult] =
    Encoder.forProduct8(
      "id",
      "company",
      "position",
      "description",
      "start_date",
      "end_date",
      "location",
      "company_url"
    )(r =>
      (r.id, r.company, r.position, r.description, r.startDate, r.endDate, r.location, r.companyUrl)
    )
}
