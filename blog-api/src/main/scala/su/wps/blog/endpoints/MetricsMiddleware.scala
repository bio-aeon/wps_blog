package su.wps.blog.endpoints

import cats.data.OptionT
import cats.effect.{Clock, MonadCancelThrow}
import org.http4s.*
import org.http4s.metrics.MetricsOps
import org.http4s.server.middleware.Metrics

private[blog] object MetricsMiddleware {

  def apply[F[_]: Clock: MonadCancelThrow](ops: MetricsOps[F])(app: HttpApp[F]): HttpApp[F] = {
    val classifyRoute = (req: Request[F]) => Option(normalizePath(req.uri.path.renderString))
    Metrics[F](ops, classifierF = classifyRoute)(app.mapF(OptionT.liftF(_))).orNotFound
  }

  private def normalizePath(path: String): String = path match {
    case p if p.matches(".*/posts/\\d+/comments$") => "/v1/posts/:id/comments"
    case p if p.matches(".*/posts/\\d+/view$") => "/v1/posts/:id/view"
    case p if p.matches(".*/posts/\\d+$") => "/v1/posts/:id"
    case p if p.matches(".*/comments/\\d+/rate$") => "/v1/comments/:id/rate"
    case p if p.matches(".*/pages/.+$") => "/v1/pages/:url"
    case other => other
  }
}
