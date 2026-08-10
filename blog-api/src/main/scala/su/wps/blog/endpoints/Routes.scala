package su.wps.blog.endpoints

import org.http4s.HttpRoutes
import sttp.tapir.AnyEndpoint

trait Routes[F[_]] {
  val routes: HttpRoutes[F]

  /** Descriptions of exactly the endpoints [[routes]] serves, so generated documentation cannot
    * drift from the implementation.
    */
  val endpoints: List[AnyEndpoint]
}
