package su.wps.blog.models.api

import io.circe.Encoder
import su.wps.blog.models.domain.SkillId

final case class SkillResult(
  id: SkillId,
  name: String,
  slug: String,
  category: String,
  proficiency: Int,
  icon: Option[String]
)

object SkillResult {
  implicit val encoder: Encoder[SkillResult] =
    Encoder.forProduct6("id", "name", "slug", "category", "proficiency", "icon")(r =>
      (r.id, r.name, r.slug, r.category, r.proficiency, r.icon)
    )
}

final case class SkillCategoryResult(
  category: String,
  skills: List[SkillResult]
)

object SkillCategoryResult {
  implicit val encoder: Encoder[SkillCategoryResult] =
    Encoder.forProduct2("category", "skills")(r => (r.category, r.skills))
}
