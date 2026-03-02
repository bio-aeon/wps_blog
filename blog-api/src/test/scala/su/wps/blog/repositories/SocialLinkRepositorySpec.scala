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

class SocialLinkRepositorySpec extends Specification with DbTest {
  sequential

  lazy val repo: SocialLinkRepository[ConnectionIO] =
    SocialLinkRepositoryImpl.create[ConnectionIO]

  implicit val genSocialLink: Gen[models.SocialLink] = arbitrary[models.SocialLink]

  "SocialLinkRepository" >> {
    "findAllActive" >> {
      "returns only active social links" >> {
        val test = for {
          _ <- insertSocialLink(id = PosInt(1), isActive = true)
          _ <- insertSocialLink(id = PosInt(2), isActive = false)
          _ <- insertSocialLink(id = PosInt(3), isActive = true)
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

      "orders by sort_order" >> {
        val test = for {
          _ <- insertSocialLink(id = PosInt(1), sortOrder = 3)
          _ <- insertSocialLink(id = PosInt(2), sortOrder = 1)
          _ <- insertSocialLink(id = PosInt(3), sortOrder = 2)
          result <- repo.findAllActive
        } yield result

        val result = test.runWithIO()
        result.map(_.id.map(_.value)) mustEqual List(Some(2), Some(3), Some(1))
      }
    }
  }

  private def insertSocialLink(
    id: PosInt = random[PosInt],
    sortOrder: Int = 0,
    isActive: Boolean = true
  ): ConnectionIO[models.SocialLink] = {
    val link = random[models.SocialLink].copy(id = id, sortOrder = sortOrder, isActive = isActive)
    models.SocialLink.sql.insert(link).map(_ => link)
  }
}
