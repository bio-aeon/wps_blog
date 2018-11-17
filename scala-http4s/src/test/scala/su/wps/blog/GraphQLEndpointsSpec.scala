package su.wps.blog

import cats.effect.IO
import cats.~>
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.specs2.matcher.MatchResult
import su.wps.blog.data.LiftFuture
import su.wps.blog.endpoints.GraphQLEndpoints

import scala.concurrent.Future

class GraphQLEndpointsSpec extends org.specs2.mutable.Specification {

  implicit val ioLiftFuture: LiftFuture[IO] = new (Future ~> IO) {
    def apply[T](fa: Future[T]): IO[T] = IO.fromFuture(IO(fa))
  }

  "GraphQLEndpoints should" >> {
    "return 200" >> {
      uriReturns200()
    }

    "return graphql test resp" >> {
      uriReturnsGraphQLTestResp()
    }
  }

  private[this] val retIndexPage: Response[IO] = {
    val jsn = Map(
      "query" -> "query { test }"
    ).asJson

    val getHW = Request[IO](Method.POST, Uri.uri("/graphql"))
      .withBody(jsn)
      .unsafeRunSync()
    val action = for {
      logger <- Slf4jLogger.create[IO]
      result <- new GraphQLEndpoints[IO].endpoints(logger).orNotFound(getHW)
    } yield result

    action.unsafeRunSync()
  }

  private[this] def uriReturns200(): MatchResult[Status] =
    retIndexPage.status must beEqualTo(Status.Ok)

  private[this] def uriReturnsGraphQLTestResp(): MatchResult[String] =
    retIndexPage.as[String].unsafeRunSync() must beEqualTo(
      "{\"data\":{\"test\":\"test resp\"}}")
}
