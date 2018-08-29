package su.wps.blog

import cats.effect.{Effect, IO}
import fs2.StreamApp
import org.http4s.server.blaze.BlazeBuilder
import su.wps.blog.endpoints.BlogEndpoints

import scala.concurrent.ExecutionContext

object Server extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]) =
    ServerStream.stream[IO]
}

object ServerStream {

  def blogEndpoints[F[_]: Effect] = new BlogEndpoints[F].endpoints

  def stream[F[_]: Effect](implicit ec: ExecutionContext) =
    BlazeBuilder[F]
      .bindHttp(8080, "0.0.0.0")
      .mountService(blogEndpoints, "/")
      .serve
}
