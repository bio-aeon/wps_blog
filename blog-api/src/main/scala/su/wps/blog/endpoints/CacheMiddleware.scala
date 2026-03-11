package su.wps.blog.endpoints

import cats.Functor
import cats.data.Kleisli
import cats.syntax.functor.*
import org.http4s.*
import org.typelevel.ci.CIString

object CacheMiddleware {

  private val CacheControlName = CIString("Cache-Control")
  private val VaryName = CIString("Vary")

  private val PostListCache = Header.Raw(CacheControlName, "public, max-age=60")
  private val PostDetailCache = Header.Raw(CacheControlName, "public, max-age=300")
  private val StaticDataCache = Header.Raw(CacheControlName, "public, max-age=3600")
  private val ShortLivedCache = Header.Raw(CacheControlName, "public, max-age=30")
  private val NoCache = Header.Raw(CacheControlName, "no-cache, no-store")
  private val VaryHeader = Header.Raw(VaryName, "Accept-Encoding")

  def apply[F[_]: Functor](routes: HttpRoutes[F]): HttpRoutes[F] =
    Kleisli { req =>
      routes.run(req).map { response =>
        if (req.method != Method.GET) response.putHeaders(NoCache)
        else addCacheHeaders(req.uri.path.renderString, response)
      }
    }

  private def addCacheHeaders[F[_]](path: String, response: Response[F]): Response[F] = {
    val cacheControl = resolveCachePolicy(path)
    response.putHeaders(cacheControl, VaryHeader)
  }

  private def resolveCachePolicy(path: String): Header.Raw = path match {
    case "/health"                                 => NoCache
    case p if p.matches(".*/posts/\\d+/comments$") => ShortLivedCache
    case p if p.matches(".*/posts/\\d+$")          => PostDetailCache
    case p if p.contains("/tags")                  => StaticDataCache
    case p if p.contains("/about")                 => StaticDataCache
    case p if p.contains("/pages")                 => StaticDataCache
    case p if p.contains("/skills")                => StaticDataCache
    case p if p.contains("/experiences")           => StaticDataCache
    case p if p.contains("/social-links")          => StaticDataCache
    case _                                         => PostListCache
  }
}
