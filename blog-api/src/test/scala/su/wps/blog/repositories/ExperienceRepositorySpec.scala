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

import java.time.LocalDate

class ExperienceRepositorySpec extends Specification with DbTest {
  sequential

  lazy val repo: ExperienceRepository[ConnectionIO] =
    ExperienceRepositoryImpl.create[ConnectionIO]

  implicit val genExperience: Gen[models.Experience] = arbitrary[models.Experience]

  implicit val arbLocalDate: org.scalacheck.Arbitrary[LocalDate] =
    org.scalacheck.Arbitrary(Gen.const(LocalDate.of(2020, 1, 1)))

  "ExperienceRepository" >> {
    "findAllActive" >> {
      "returns only active experiences" >> {
        val test = for {
          _ <- insertExperience(id = PosInt(1), isActive = true)
          _ <- insertExperience(id = PosInt(2), isActive = false)
          _ <- insertExperience(id = PosInt(3), isActive = true)
          result <- repo.findAllActive
        } yield result

        val result = test.runWithIO()
        result must have size 2
      }

      "returns empty list when none exist" >> {
        val test = repo.findAllActive

        val result = test.runWithIO()
        result must beEmpty
      }

      "orders by sort_order then start_date desc" >> {
        val test = for {
          _ <- insertExperience(id = PosInt(1), sortOrder = 1, startDate = LocalDate.of(2020, 1, 1))
          _ <- insertExperience(id = PosInt(2), sortOrder = 1, startDate = LocalDate.of(2022, 6, 1))
          _ <- insertExperience(id = PosInt(3), sortOrder = 0, startDate = LocalDate.of(2019, 1, 1))
          result <- repo.findAllActive
        } yield result

        val result = test.runWithIO()
        result.map(_.id.map(_.value)) mustEqual List(Some(3), Some(2), Some(1))
      }

      "handles null end_date" >> {
        val test = for {
          _ <- insertExperience(id = PosInt(1), endDate = None)
          result <- repo.findAllActive
        } yield result

        val result = test.runWithIO()
        result must have size 1
        result.head.endDate must beNone
      }
    }
  }

  private def insertExperience(
    id: PosInt = random[PosInt],
    sortOrder: Int = 0,
    isActive: Boolean = true,
    startDate: LocalDate = LocalDate.of(2020, 1, 1),
    endDate: Option[LocalDate] = None
  ): ConnectionIO[models.Experience] = {
    val exp = random[models.Experience].copy(
      id = id,
      sortOrder = sortOrder,
      isActive = isActive,
      startDate = startDate,
      endDate = endDate
    )
    models.Experience.sql.insert(exp).map(_ => exp)
  }
}
