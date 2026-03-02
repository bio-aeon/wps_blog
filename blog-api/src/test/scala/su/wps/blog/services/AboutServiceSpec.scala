package su.wps.blog.services

import cats.Id
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.*
import su.wps.blog.services.mocks.*
import tofu.doobie.transactor.Txr

import java.time.{LocalDate, ZonedDateTime}

class AboutServiceSpec extends Specification {
  type RunF[A] = Either[Throwable, A]

  val xa: Txr[RunF, Id] = TxrMock.create[RunF]
  private val now = ZonedDateTime.now()

  "AboutService.getAboutPage" >> {
    "composes all sub-results into AboutResult" >> {
      val skills =
        List(Skill("Scala", "scala", "Backend", 90, None, 0, true, now, Some(SkillId(1))))
      val experiences = List(
        Experience(
          "Acme",
          "Engineer",
          "Description",
          LocalDate.of(2020, 1, 1),
          None,
          Some("Remote"),
          None,
          0,
          true,
          now,
          Some(ExperienceId(1))
        )
      )
      val socialLinks = List(
        SocialLink(
          "github",
          "https://github.com",
          Some("GitHub"),
          None,
          0,
          true,
          now,
          Some(SocialLinkId(1))
        )
      )
      val configs = List(
        Config("profile_name", "John", "", now, Some(ConfigId(1))),
        Config("profile_title", "Engineer", "", now, Some(ConfigId(2)))
      )
      val aboutPage = Some(Page("about", "About", "Bio content", now, Some(PageId(1))))

      val service = mkService(skills, experiences, socialLinks, configs, aboutPage)

      service.getAboutPage must beRight.which { result =>
        result.profile.name == "John" &&
        result.profile.title == "Engineer" &&
        result.profile.bio == "Bio content" &&
        result.skills.nonEmpty &&
        result.experiences.nonEmpty &&
        result.socialLinks.nonEmpty
      }
    }

    "handles empty sections" >> {
      val service = mkService()

      service.getAboutPage must beRight.which { result =>
        result.skills.isEmpty &&
        result.experiences.isEmpty &&
        result.socialLinks.isEmpty
      }
    }

    "handles missing configs with empty string defaults" >> {
      val service = mkService()

      service.getAboutPage must beRight.which { result =>
        result.profile.name == "" &&
        result.profile.title == "" &&
        result.profile.photoUrl == "" &&
        result.profile.resumeUrl == ""
      }
    }

    "handles missing about page with empty bio" >> {
      val configs = List(Config("profile_name", "Jane", "", now, Some(ConfigId(1))))
      val service = mkService(configs = configs, aboutPage = None)

      service.getAboutPage must beRight.which { result =>
        result.profile.name == "Jane" &&
        result.profile.bio == ""
      }
    }

    "groups skills by category in result" >> {
      val skills = List(
        Skill("Scala", "scala", "Backend", 90, None, 0, true, now, Some(SkillId(1))),
        Skill("Rust", "rust", "Backend", 80, None, 1, true, now, Some(SkillId(2))),
        Skill("React", "react", "Frontend", 70, None, 0, true, now, Some(SkillId(3)))
      )
      val service = mkService(skills = skills)

      service.getAboutPage must beRight.which { result =>
        result.skills.size == 2 &&
        result.skills.map(_.category).sorted == List("Backend", "Frontend")
      }
    }
  }

  private def mkService(
    skills: List[Skill] = Nil,
    experiences: List[Experience] = Nil,
    socialLinks: List[SocialLink] = Nil,
    configs: List[Config] = Nil,
    aboutPage: Option[Page] = None
  ): AboutService[RunF] = {
    val skillRepo = SkillRepositoryMock.create[Id](skills)
    val experienceRepo = ExperienceRepositoryMock.create[Id](experiences)
    val socialLinkRepo = SocialLinkRepositoryMock.create[Id](socialLinks)
    val configRepo = ConfigRepositoryMock.create[Id](findByNamesResult = configs)
    val pageRepo = PageRepositoryMock.create[Id](findByUrlResult = aboutPage)
    AboutServiceImpl.create[RunF, Id](
      skillRepo,
      experienceRepo,
      socialLinkRepo,
      configRepo,
      pageRepo,
      xa
    )
  }
}
