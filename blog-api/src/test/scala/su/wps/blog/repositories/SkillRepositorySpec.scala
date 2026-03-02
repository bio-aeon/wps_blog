package su.wps.blog.repositories

import doobie.ConnectionIO
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless.*
import org.specs2.mutable.Specification
import su.wps.blog.tools.DbTest
import su.wps.blog.tools.scalacheck.*
import su.wps.blog.tools.syntax.*
import su.wps.blog.tools.types.{PosInt, Varchar}

class SkillRepositorySpec extends Specification with DbTest {
  sequential

  lazy val repo: SkillRepository[ConnectionIO] = SkillRepositoryImpl.create[ConnectionIO]

  implicit val genSkill: Gen[models.Skill] = arbitrary[models.Skill]

  "SkillRepository" >> {
    "findAllActive" >> {
      "returns only active skills" >> {
        val test = for {
          _ <- insertSkill(id = PosInt(1), slug = "scala", isActive = true)
          _ <- insertSkill(id = PosInt(2), slug = "rust", isActive = false)
          _ <- insertSkill(id = PosInt(3), slug = "java", isActive = true)
          result <- repo.findAllActive
        } yield result

        val result = test.runWithIO()
        result must have size 2
        result.map(_.slug) must contain(allOf("scala", "java"))
      }

      "returns empty list when no skills exist" >> {
        val test = repo.findAllActive

        val result = test.runWithIO()
        result must beEmpty
      }

      "orders by sort_order then name" >> {
        val test = for {
          _ <- insertSkill(id = PosInt(1), slug = "zebra", name = "Zebra", sortOrder = 2)
          _ <- insertSkill(id = PosInt(2), slug = "alpha", name = "Alpha", sortOrder = 1)
          _ <- insertSkill(id = PosInt(3), slug = "beta", name = "Beta", sortOrder = 1)
          result <- repo.findAllActive
        } yield result

        val result = test.runWithIO()
        result.map(_.name) mustEqual List("Alpha", "Beta", "Zebra")
      }

      "excludes inactive skills" >> {
        val test = for {
          _ <- insertSkill(id = PosInt(1), slug = "inactive1", isActive = false)
          _ <- insertSkill(id = PosInt(2), slug = "inactive2", isActive = false)
          result <- repo.findAllActive
        } yield result

        val result = test.runWithIO()
        result must beEmpty
      }
    }
  }

  private def insertSkill(
    id: PosInt = random[PosInt],
    slug: String = s"skill-${random[PosInt].value}",
    name: String = "Skill",
    category: String = "Backend",
    sortOrder: Int = 0,
    isActive: Boolean = true
  ): ConnectionIO[models.Skill] = {
    val skill = random[models.Skill].copy(
      id = id,
      slug = Varchar(slug),
      name = Varchar(name),
      category = Varchar(category),
      proficiency = 50,
      sortOrder = sortOrder,
      isActive = isActive
    )
    models.Skill.sql.insert(skill).map(_ => skill)
  }
}
