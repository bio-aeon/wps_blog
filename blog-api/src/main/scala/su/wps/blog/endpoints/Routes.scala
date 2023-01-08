package su.wps.blog.endpoints

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

final class Routes[F[_]: Monad] private () extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root => Ok("test")
  }
}

object Routes {

  def create[F[_]: Monad]: Routes[F] =
    new Routes[F]
}
