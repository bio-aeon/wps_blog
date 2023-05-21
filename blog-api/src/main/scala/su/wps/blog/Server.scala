package su.wps.blog

import cats.effect._
import com.comcast.ip4s.{Ipv4Address, Port}
import fs2.io.net.Network
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import su.wps.blog.endpoints.Routes

object Server extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val routes = Routes.create[IO]
    mkHttpServer[IO](routes.routes).use(_ => IO.never).as(ExitCode.Success)
  }

  private def mkHttpServer[F[_]: Async: Network](routes: HttpRoutes[F]): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(Ipv4Address.fromString("0.0.0.0").getOrElse(throw new Exception("Incorrect host.")))
      .withPort(Port.fromInt(8080).getOrElse(throw new Exception("Incorrect port.")))
      .withHttpApp(routes.orNotFound)
      .build
}
