package su.wps.blog.services

import cats.Id
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.{SocialLink, SocialLinkId}
import su.wps.blog.services.mocks.{SocialLinkRepositoryMock, TxrMock}
import tofu.doobie.transactor.Txr

import java.time.ZonedDateTime

class SocialLinkServiceSpec extends Specification {
  type RunF[A] = Either[Throwable, A]

  val xa: Txr[RunF, Id] = TxrMock.create[RunF]

  "SocialLinkService.getSocialLinks" >> {
    "transforms domain SocialLink to SocialLinkResult" >> {
      val links =
        List(mkLink(1, "github", "https://github.com/user", Some("GitHub"), Some("gh-icon")))
      val service = mkService(links)

      service.getSocialLinks must beRight.which { result =>
        result.size == 1 &&
        result.head.id.value == 1 &&
        result.head.platform == "github" &&
        result.head.url == "https://github.com/user" &&
        result.head.label.contains("GitHub") &&
        result.head.icon.contains("gh-icon")
      }
    }

    "returns empty list when no social links exist" >> {
      val service = mkService(Nil)

      service.getSocialLinks must beRight.which(_.isEmpty)
    }

    "preserves order from repository" >> {
      val links = List(
        mkLink(1, "github", "https://github.com"),
        mkLink(2, "linkedin", "https://linkedin.com"),
        mkLink(3, "twitter", "https://twitter.com")
      )
      val service = mkService(links)

      service.getSocialLinks must beRight.which { result =>
        result.map(_.platform) == List("github", "linkedin", "twitter")
      }
    }
  }

  private def mkLink(
    id: Int,
    platform: String,
    url: String,
    label: Option[String] = None,
    icon: Option[String] = None
  ): SocialLink =
    SocialLink(
      platform = platform,
      url = url,
      label = label,
      icon = icon,
      sortOrder = 0,
      isActive = true,
      createdAt = ZonedDateTime.now(),
      id = Some(SocialLinkId(id))
    )

  private def mkService(links: List[SocialLink]): SocialLinkService[RunF] = {
    val repo = SocialLinkRepositoryMock.create[Id](links)
    SocialLinkServiceImpl.create[RunF, Id](repo, xa)
  }
}
