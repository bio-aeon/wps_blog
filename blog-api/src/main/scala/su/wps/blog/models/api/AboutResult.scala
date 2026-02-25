package su.wps.blog.models.api

import io.circe.Encoder

final case class ProfileResult(
  name: String,
  title: String,
  photoUrl: String,
  resumeUrl: String,
  bio: String
)

object ProfileResult {
  implicit val encoder: Encoder[ProfileResult] =
    Encoder.forProduct5("name", "title", "photo_url", "resume_url", "bio")(r =>
      (r.name, r.title, r.photoUrl, r.resumeUrl, r.bio)
    )
}

final case class AboutResult(
  profile: ProfileResult,
  skills: List[SkillCategoryResult],
  experiences: List[ExperienceResult],
  socialLinks: List[SocialLinkResult],
  testimonials: List[TestimonialResult]
)

object AboutResult {
  implicit val encoder: Encoder[AboutResult] =
    Encoder.forProduct5(
      "profile",
      "skills",
      "experiences",
      "social_links",
      "testimonials"
    )(r => (r.profile, r.skills, r.experiences, r.socialLinks, r.testimonials))
}
