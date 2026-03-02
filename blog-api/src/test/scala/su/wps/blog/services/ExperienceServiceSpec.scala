package su.wps.blog.services

import cats.Id
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.{Experience, ExperienceId}
import su.wps.blog.services.mocks.{ExperienceRepositoryMock, TxrMock}
import tofu.doobie.transactor.Txr

import java.time.{LocalDate, ZonedDateTime}

class ExperienceServiceSpec extends Specification {
  type RunF[A] = Either[Throwable, A]

  val xa: Txr[RunF, Id] = TxrMock.create[RunF]

  "ExperienceService.getExperiences" >> {
    "transforms domain Experience to ExperienceResult" >> {
      val experiences = List(
        mkExperience(
          1,
          "Acme Corp",
          "Engineer",
          LocalDate.of(2020, 1, 1),
          Some(LocalDate.of(2022, 6, 1))
        )
      )
      val service = mkService(experiences)

      service.getExperiences must beRight.which { result =>
        result.size == 1 &&
        result.head.id.value == 1 &&
        result.head.company == "Acme Corp" &&
        result.head.position == "Engineer" &&
        result.head.startDate == LocalDate.of(2020, 1, 1) &&
        result.head.endDate.contains(LocalDate.of(2022, 6, 1))
      }
    }

    "handles null end_date (current position)" >> {
      val experiences =
        List(mkExperience(1, "Current Co", "Developer", LocalDate.of(2023, 1, 1), None))
      val service = mkService(experiences)

      service.getExperiences must beRight.which { result =>
        result.head.endDate.isEmpty
      }
    }

    "returns empty list when no experiences exist" >> {
      val service = mkService(Nil)

      service.getExperiences must beRight.which(_.isEmpty)
    }

    "preserves order from repository" >> {
      val experiences = List(
        mkExperience(1, "First", "Role1", LocalDate.of(2020, 1, 1)),
        mkExperience(2, "Second", "Role2", LocalDate.of(2021, 1, 1))
      )
      val service = mkService(experiences)

      service.getExperiences must beRight.which { result =>
        result.map(_.company) == List("First", "Second")
      }
    }
  }

  private def mkExperience(
    id: Int,
    company: String,
    position: String,
    startDate: LocalDate,
    endDate: Option[LocalDate] = None
  ): Experience =
    Experience(
      company = company,
      position = position,
      description = "Description",
      startDate = startDate,
      endDate = endDate,
      location = Some("Remote"),
      companyUrl = Some("https://example.com"),
      sortOrder = 0,
      isActive = true,
      createdAt = ZonedDateTime.now(),
      id = Some(ExperienceId(id))
    )

  private def mkService(experiences: List[Experience]): ExperienceService[RunF] = {
    val repo = ExperienceRepositoryMock.create[Id](experiences)
    ExperienceServiceImpl.create[RunF, Id](repo, xa)
  }
}
