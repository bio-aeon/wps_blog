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

class PageRepositorySpec extends Specification with DbTest {
  sequential

  lazy val repo: PageRepository[ConnectionIO] = PageRepositoryImpl.create[ConnectionIO]

  implicit val genPage: Gen[models.Page] = arbitrary[models.Page]

  "PageRepository should" >> {
    "findByUrl returns the page if it exists" >> {
      val test = for {
        page <- insertPage(url = "about")
        result <- repo.findByUrl("about")
      } yield result

      val result = test.runWithIO()
      result must beSome.which(_.url == "about")
    }

    "findByUrl returns None for non-existent url" >> {
      val test = repo.findByUrl("non-existent-url")

      val result = test.runWithIO()
      result must beNone
    }

    "findByUrl returns correct page when multiple pages exist" >> {
      val test = for {
        _ <- insertPage(id = PosInt(1), url = "about")
        _ <- insertPage(id = PosInt(2), url = "contact")
        _ <- insertPage(id = PosInt(3), url = "privacy")
        result <- repo.findByUrl("contact")
      } yield result

      val result = test.runWithIO()
      result must beSome.which(_.url == "contact")
    }

    "findAll returns all pages" >> {
      val test = for {
        _ <- insertPage(id = PosInt(1), url = "about")
        _ <- insertPage(id = PosInt(2), url = "contact")
        _ <- insertPage(id = PosInt(3), url = "privacy")
        result <- repo.findAll
      } yield result

      val result = test.runWithIO()
      result must have size 3
    }

    "findAll returns empty list when no pages exist" >> {
      val test = repo.findAll

      val result = test.runWithIO()
      result must beEmpty
    }

    "findAll returns pages ordered by title" >> {
      val test = for {
        _ <- insertPage(id = PosInt(1), url = "zebra", title = "Zebra Page")
        _ <- insertPage(id = PosInt(2), url = "about", title = "About Us")
        _ <- insertPage(id = PosInt(3), url = "contact", title = "Contact")
        result <- repo.findAll
      } yield result

      val result = test.runWithIO()
      result.map(_.title) mustEqual List("About Us", "Contact", "Zebra Page")
    }
  }

  private def insertPage(
    id: PosInt = random[PosInt],
    url: String,
    title: String = "Default Title"
  ): ConnectionIO[models.Page] = {
    val page = random[models.Page].copy(id = id, url = Varchar(url), title = Varchar(title))
    models.Page.sql.insert(page).map(_ => page)
  }
}
