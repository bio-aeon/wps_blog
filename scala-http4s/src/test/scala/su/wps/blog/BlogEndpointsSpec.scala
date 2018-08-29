package su.wps.blog

import cats.effect.IO
import org.http4s._
import org.http4s.implicits._
import org.specs2.matcher.MatchResult
import su.wps.blog.endpoints.BlogEndpoints

class BlogEndpointsSpec extends org.specs2.mutable.Specification {

  "BlogEndpoints should" >> {
    "return 200" >> {
      uriReturns200()
    }
    "return index page text" >> {
      uriReturnsIndexPageText()
    }
  }

  private[this] val retIndexPage: Response[IO] = {
    val getHW = Request[IO](Method.GET, Uri.uri("/"))
    new BlogEndpoints[IO].endpoints.orNotFound(getHW).unsafeRunSync()
  }

  private[this] def uriReturns200(): MatchResult[Status] =
    retIndexPage.status must beEqualTo(Status.Ok)

  private[this] def uriReturnsIndexPageText(): MatchResult[String] =
    retIndexPage.as[String].unsafeRunSync() must beEqualTo("{\"message\":\"Index page\"}")
}
