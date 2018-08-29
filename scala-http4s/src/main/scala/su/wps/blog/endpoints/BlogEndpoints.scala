package su.wps.blog.endpoints

import cats.effect.Effect
import io.circe.Json
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class BlogEndpoints[F[_]: Effect] extends Http4sDsl[F] {
  def endpoints: HttpService[F] = {
    HttpService[F] {
      case GET -> Root =>
        Ok(Json.obj("message" -> Json.fromString("Index page")))
    }
  }
}
