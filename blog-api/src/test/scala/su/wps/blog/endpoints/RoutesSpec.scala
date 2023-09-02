package su.wps.blog.endpoints

import cats.Monad
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.http4s._
import org.http4s.implicits._
import org.specs2.mutable.Specification
import su.wps.blog.endpoints.mocks.PostServiceMock
import su.wps.blog.models.api.{ListPostResult, PostResult}
import su.wps.blog.models.domain.{AppErr, PostId}
import tofu.Raise

import java.time.ZonedDateTime

class RoutesSpec extends Specification {

  "Api routes should" >> {
    "return correct code and body for posts list retrieving" >> {
      val routes = mkRoutes[IO]
      val request =
        Request[IO](Method.GET, uri"posts".withQueryParams(Map("limit" -> "10", "offset" -> "0")))

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"items":[{"id":1,"name":"name","short_text":"text","created_at":"2001-01-01T09:15:00Z"}],"total":1}"""
    }

    "return correct code and body for post retrieving by id" >> {
      val routes = mkRoutes[IO]
      val request = Request[IO](Method.GET, uri"posts/1")

      val resp = routes.routes.run(request).value.map(_.get).unsafeRunSync()
      val respBody = resp.as[String].unsafeRunSync()

      resp.status mustEqual Status.Ok
      respBody mustEqual """{"name":"name","text":"text","created_at":"2001-01-01T09:15:00Z"}"""
    }
  }

  private def mkRoutes[F[_]: Monad: Raise[*[_], AppErr]]: Routes[F] = {
    val postService = PostServiceMock.create[F](
      List(ListPostResult(PostId(1), "name", "text", ZonedDateTime.parse("2001-01-01T09:15:00Z"))),
      Some(PostResult("name", "text", ZonedDateTime.parse("2001-01-01T09:15:00Z")))
    )

    RoutesImpl.create[F](postService)
  }
}
