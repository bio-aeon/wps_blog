package su.wps.blog.endpoints

import cats.effect.Async
import org.http4s.HttpRoutes
import sttp.tapir.AnyEndpoint
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object SwaggerRoutes {

  private val ApiTitle = "WPS Blog API"
  private val ApiVersion = "1.0.0"

  def routes[F[_]: Async](endpoints: List[AnyEndpoint]): HttpRoutes[F] = {
    val swaggerEndpoints = SwaggerInterpreter()
      .fromEndpoints[F](endpoints, ApiTitle, ApiVersion)

    Http4sServerInterpreter[F]().toRoutes(swaggerEndpoints)
  }
}
