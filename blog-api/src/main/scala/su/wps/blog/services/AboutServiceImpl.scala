package su.wps.blog.services

import cats.Monad
import cats.syntax.apply.*
import cats.syntax.functor.*
import io.scalaland.chimney.dsl.*
import mouse.anyf.*
import su.wps.blog.models.api.*
import su.wps.blog.repositories.*
import tofu.doobie.transactor.Txr

final class AboutServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  skillRepo: SkillRepository[DB],
  experienceRepo: ExperienceRepository[DB],
  socialLinkRepo: SocialLinkRepository[DB],
  configRepo: ConfigRepository[DB],
  pageRepo: PageRepository[DB],
  xa: Txr[F, DB]
) extends AboutService[F] {

  private val ProfileConfigNames =
    List("profile_name", "profile_title", "profile_photo_url", "resume_url")

  def getAboutPage: F[AboutResult] = {
    val fetchAll = (
      skillRepo.findAllActive,
      experienceRepo.findAllActive,
      socialLinkRepo.findAllActive,
      configRepo.findByNames(ProfileConfigNames),
      pageRepo.findByUrl("about")
    ).mapN { (skills, experiences, socialLinks, configs, aboutPage) =>
      val configMap = configs.map(c => c.name -> c.value).toMap
      val profile = ProfileResult(
        name = configMap.getOrElse("profile_name", ""),
        title = configMap.getOrElse("profile_title", ""),
        photoUrl = configMap.getOrElse("profile_photo_url", ""),
        resumeUrl = configMap.getOrElse("resume_url", ""),
        bio = aboutPage.map(_.content).getOrElse("")
      )
      AboutResult(
        profile = profile,
        skills = groupSkillsByCategory(skills),
        experiences = experiences.map(transformExperience),
        socialLinks = socialLinks.map(transformSocialLink)
      )
    }

    fetchAll.thrushK(xa.trans)
  }

  private def groupSkillsByCategory(
    skills: List[su.wps.blog.models.domain.Skill]
  ): List[SkillCategoryResult] =
    skills
      .groupBy(_.category)
      .toList
      .sortBy(_._1)
      .map { case (category, categorySkills) =>
        SkillCategoryResult(
          category,
          categorySkills
            .sortBy(_.sortOrder)
            .map(_.into[SkillResult].withFieldComputed(_.id, _.nonEmptyId).transform)
        )
      }

  private def transformExperience(exp: su.wps.blog.models.domain.Experience): ExperienceResult =
    exp.into[ExperienceResult].withFieldComputed(_.id, _.nonEmptyId).transform

  private def transformSocialLink(link: su.wps.blog.models.domain.SocialLink): SocialLinkResult =
    link.into[SocialLinkResult].withFieldComputed(_.id, _.nonEmptyId).transform
}

object AboutServiceImpl {
  def create[F[_]: Monad, DB[_]: Monad](
    skillRepo: SkillRepository[DB],
    experienceRepo: ExperienceRepository[DB],
    socialLinkRepo: SocialLinkRepository[DB],
    configRepo: ConfigRepository[DB],
    pageRepo: PageRepository[DB],
    xa: Txr[F, DB]
  ): AboutServiceImpl[F, DB] =
    new AboutServiceImpl(skillRepo, experienceRepo, socialLinkRepo, configRepo, pageRepo, xa)
}
