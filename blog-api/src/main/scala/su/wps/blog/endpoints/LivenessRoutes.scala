package su.wps.blog.endpoints

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

object LivenessRoutes {

  def routes[F[_]](implicit F: Sync[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl.*

    HttpRoutes.of[F] { case GET -> Root / "health" / "live" =>
      Ok("""{"status":"alive"}""")
    }
  }
}
