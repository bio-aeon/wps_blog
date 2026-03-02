package su.wps.blog.services

import cats.Id
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.{Skill, SkillId}
import su.wps.blog.services.mocks.{SkillRepositoryMock, TxrMock}
import tofu.doobie.transactor.Txr

import java.time.ZonedDateTime

class SkillServiceSpec extends Specification {
  type RunF[A] = Either[Throwable, A]

  val xa: Txr[RunF, Id] = TxrMock.create[RunF]

  "SkillService.getSkillsByCategory" >> {
    "groups skills by category" >> {
      val skills = List(
        mkSkill(1, "Scala", "Backend", sortOrder = 0),
        mkSkill(2, "Rust", "Backend", sortOrder = 1),
        mkSkill(3, "React", "Frontend", sortOrder = 0)
      )
      val service = mkService(skills)

      service.getSkillsByCategory must beRight.which { result =>
        result.size == 2 &&
        result.map(_.category).sorted == List("Backend", "Frontend")
      }
    }

    "returns empty list when no skills exist" >> {
      val service = mkService(Nil)

      service.getSkillsByCategory must beRight.which(_.isEmpty)
    }

    "sorts categories alphabetically" >> {
      val skills = List(
        mkSkill(1, "Docker", "DevOps"),
        mkSkill(2, "Scala", "Backend"),
        mkSkill(3, "React", "Frontend")
      )
      val service = mkService(skills)

      service.getSkillsByCategory must beRight.which { result =>
        result.map(_.category) == List("Backend", "DevOps", "Frontend")
      }
    }

    "preserves sort order within groups" >> {
      val skills = List(
        mkSkill(1, "Java", "Backend", sortOrder = 2),
        mkSkill(2, "Scala", "Backend", sortOrder = 0),
        mkSkill(3, "Kotlin", "Backend", sortOrder = 1)
      )
      val service = mkService(skills)

      service.getSkillsByCategory must beRight.which { result =>
        result.head.skills.map(_.name) == List("Scala", "Kotlin", "Java")
      }
    }

    "transforms domain Skill to SkillResult with correct fields" >> {
      val skills = List(mkSkill(42, "Scala", "Backend", proficiency = 90, icon = Some("S")))
      val service = mkService(skills)

      service.getSkillsByCategory must beRight.which { result =>
        val skill = result.head.skills.head
        skill.id.value == 42 &&
        skill.name == "Scala" &&
        skill.proficiency == 90 &&
        skill.icon.contains("S")
      }
    }
  }

  private def mkSkill(
    id: Int,
    name: String,
    category: String,
    sortOrder: Int = 0,
    proficiency: Int = 50,
    icon: Option[String] = None
  ): Skill =
    Skill(
      name = name,
      slug = name.toLowerCase,
      category = category,
      proficiency = proficiency,
      icon = icon,
      sortOrder = sortOrder,
      isActive = true,
      createdAt = ZonedDateTime.now(),
      id = Some(SkillId(id))
    )

  private def mkService(skills: List[Skill]): SkillService[RunF] = {
    val repo = SkillRepositoryMock.create[Id](skills)
    SkillServiceImpl.create[RunF, Id](repo, xa)
  }
}
