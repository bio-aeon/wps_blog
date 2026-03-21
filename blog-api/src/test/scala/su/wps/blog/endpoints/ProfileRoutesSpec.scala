package su.wps.blog.endpoints

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import org.specs2.mutable.Specification
import su.wps.blog.models.api.*
import su.wps.blog.models.domain.PostId

class ProfileRoutesSpec extends Specification with RoutesSpecSupport with CatsEffect {

  "Profile routes" >> {
    "GET /skills" >> {
      "returns skills grouped by category" >> {
        val routes = buildRoutes[IO](skillsResult = testSkillCategories)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/skills"))

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.Ok
          respBody must contain("\"category\":")
          respBody must contain("\"skills\":")
        }
      }
    }

    "GET /experiences" >> {
      "returns experiences" >> {
        val routes = buildRoutes[IO](experiencesResult = testExperiences)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/experiences"))

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.Ok
          respBody must contain("\"company\":")
          respBody must contain("\"position\":")
        }
      }
    }

    "GET /social-links" >> {
      "returns social links" >> {
        val routes = buildRoutes[IO](socialLinksResult = testSocialLinks)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/social-links"))

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.Ok
          respBody must contain("\"platform\":")
          respBody must contain("\"url\":")
        }
      }
    }

    "POST /contact" >> {
      "returns success" >> {
        val routes = buildRoutes[IO]()
        val body =
          CreateContactRequest("John", "john@example.com", "Hello", "Test message body", None)
        val request =
          Request[IO](Method.POST, Uri.unsafeFromString(s"$v1/contact")).withEntity(body.asJson)

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.Ok
          respBody must contain("\"message\":")
        }
      }
    }

    "GET /feed" >> {
      "returns feed data" >> {
        val feedData = FeedResult(
          List(
            FeedPostItem(
              PostId(1),
              "Test Post",
              Some("Short text"),
              Some("Meta desc"),
              testTimestamp,
              testLang,
              testTags,
              testAvailableLangs
            )
          ),
          List(FeedPageItem("about", "About Us", testTimestamp)),
          List(FeedTagItem("scala", "scala"))
        )
        val routes = buildRoutes[IO](feedResult = feedData)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/feed"))

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.Ok
          respBody must contain("\"posts\":")
          respBody must contain("\"pages\":")
          respBody must contain("\"tags\":")
        }
      }

      "returns empty feed when no data" >> {
        val routes = buildRoutes[IO]()
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/feed"))

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.Ok
          respBody mustEqual """{"posts":[],"pages":[],"tags":[]}"""
        }
      }
    }

    "GET /about" >> {
      "returns about page data" >> {
        val routes = buildRoutes[IO](aboutResult = testAbout)
        val request = Request[IO](Method.GET, Uri.unsafeFromString(s"$v1/about"))

        for {
          resp <- routes.routes.run(request).value.map(_.get)
          respBody <- resp.as[String]
        } yield {
          resp.status mustEqual Status.Ok
          respBody must contain("\"profile\":")
          respBody must contain("\"skills\":")
          respBody must contain("\"experiences\":")
          respBody must contain("\"social_links\":")
        }
      }
    }
  }

}
