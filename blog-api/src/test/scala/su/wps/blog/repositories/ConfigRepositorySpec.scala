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

class ConfigRepositorySpec extends Specification with DbTest {
  sequential

  lazy val repo: ConfigRepository[ConnectionIO] = ConfigRepositoryImpl.create[ConnectionIO]

  implicit val genConfig: Gen[models.Config] = arbitrary[models.Config]

  "ConfigRepository" >> {
    "findByName" >> {
      "returns existing config" >> {
        val test = for {
          _ <- insertConfig(id = PosInt(1), name = "profile_name", value = "John Doe")
          result <- repo.findByName("profile_name")
        } yield result

        val result = test.runWithIO()
        result must beSome.which(_.value == "John Doe")
      }

      "returns None for missing config" >> {
        val test = repo.findByName("nonexistent_key")

        val result = test.runWithIO()
        result must beNone
      }

    }

    "findByNames" >> {
      "returns all matching configs" >> {
        val test = for {
          _ <- insertConfig(id = PosInt(1), name = "profile_name", value = "John")
          _ <- insertConfig(id = PosInt(2), name = "profile_title", value = "Engineer")
          _ <- insertConfig(id = PosInt(3), name = "other_key", value = "other")
          result <- repo.findByNames(List("profile_name", "profile_title"))
        } yield result

        val result = test.runWithIO()
        result must have size 2
        result.map(_.name) must contain(allOf("profile_name", "profile_title"))
      }
    }
  }

  private def insertConfig(
    id: PosInt = random[PosInt],
    name: String,
    value: String = ""
  ): ConnectionIO[models.Config] = {
    val config = random[models.Config].copy(id = id, name = Varchar(name), value = Varchar(value))
    models.Config.sql.insert(config).map(_ => config)
  }
}
