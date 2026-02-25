package su.wps.blog.models.domain

import io.circe.Encoder

import java.time.ZonedDateTime

final case class Skill(
  name: String,
  slug: String,
  category: String,
  proficiency: Int,
  icon: Option[String],
  sortOrder: Int,
  isActive: Boolean,
  createdAt: ZonedDateTime,
  id: Option[SkillId] = None
) {
  def nonEmptyId: SkillId = id.getOrElse(throw new IllegalStateException("Empty skill id"))
}

final case class SkillId(value: Int) extends AnyVal

object SkillId {
  implicit val encoder: Encoder[SkillId] = Encoder[Int].contramap(_.value)
}
