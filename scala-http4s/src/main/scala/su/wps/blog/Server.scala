package su.wps.blog

import cats.effect.{Effect, IO}
import cats.~>
import fs2.{Stream, StreamApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.http4s.server.blaze.BlazeBuilder
import su.wps.blog.data.LiftFuture
import su.wps.blog.endpoints.GraphQLEndpoints

import scala.concurrent.{ExecutionContext, Future}

object Server extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit lazy val ioLiftFuture: LiftFuture[IO] = new (Future ~> IO) {
    def apply[T](fa: Future[T]): IO[T] = IO.fromFuture(IO(fa))
  }

  def stream(args: List[String], requestShutdown: IO[Unit]) =
    ServerStream.stream[IO]
}

object ServerStream {

  def stream[F[_]: Effect: LiftFuture](implicit ec: ExecutionContext) =
    for {
      logger <- Stream.eval(Slf4jLogger.create[F])
      exitCode <- BlazeBuilder[F]
        .bindHttp(8080, "0.0.0.0")
        .mountService(new GraphQLEndpoints[F].endpoints(logger), "/")
        .serve
    } yield exitCode
}
