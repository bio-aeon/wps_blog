package su.wps.blog

import cats.effect.{Effect, IO}
import cats.~>
import fs2.StreamApp
import org.http4s.server.blaze.BlazeBuilder
import su.wps.blog.data.TaglessFuture
import su.wps.blog.endpoints.GraphQLEndpoints

import scala.concurrent.{ExecutionContext, Future}

object Server extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit lazy val ioTaglessFuture: TaglessFuture[IO] = new (Future ~> IO) {
    def apply[T](fa: Future[T]): IO[T] = IO.fromFuture(IO(fa))
  }

  def stream(args: List[String], requestShutdown: IO[Unit]) =
    ServerStream.stream[IO]
}

object ServerStream {

  def graphQLEndpoints[F[_]: Effect: TaglessFuture] =
    new GraphQLEndpoints[F].endpoints

  def stream[F[_]: Effect: TaglessFuture](implicit ec: ExecutionContext) =
    BlazeBuilder[F]
      .bindHttp(8080, "0.0.0.0")
      .mountService(graphQLEndpoints, "/")
      .serve
}
