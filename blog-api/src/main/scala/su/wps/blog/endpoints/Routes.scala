package su.wps.blog.endpoints

import org.http4s.HttpRoutes

trait Routes[F[_]] {
  val routes: HttpRoutes[F]
}
