package su.wps.blog.endpoints

import cats.effect.Concurrent
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.circe.syntax.*
import org.http4s.{HttpRoutes, Request}
import org.http4s.circe.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`X-Forwarded-For`
import org.http4s.server.Router
import su.wps.blog.models.api.{CreateCommentRequest, CreateContactRequest, RateCommentRequest}
import su.wps.blog.models.domain.{AppErr, CommentId, PostId}
import su.wps.blog.services.*
import su.wps.blog.validation.Validation

final class RoutesImpl[F[_]: Concurrent] private (
  postService: PostService[F],
  commentService: CommentService[F],
  tagService: TagService[F],
  pageService: PageService[F],
  healthService: HealthService[F],
  skillService: SkillService[F],
  experienceService: ExperienceService[F],
  socialLinkService: SocialLinkService[F],
  contactService: ContactService[F],
  aboutService: AboutService[F],
  feedService: FeedService[F],
  languageService: LanguageService[F]
) extends Http4sDsl[F]
    with Routes[F] {
  import RoutesImpl._

  private object LimitParamMatcher extends QueryParamDecoderMatcher[Int]("limit")
  private object OffsetParamMatcher extends QueryParamDecoderMatcher[Int]("offset")
  private object TagParamMatcher extends OptionalQueryParamDecoderMatcher[String]("tag")
  private object QueryParamMatcher extends QueryParamDecoderMatcher[String]("q")
  private object CountParamMatcher extends OptionalQueryParamDecoderMatcher[Int]("count")
  private object LangParamMatcher extends OptionalQueryParamDecoderMatcher[String]("lang")

  private def resolveLang(req: Request[F], explicit: Option[String]): F[String] = {
    val acceptLang = req.headers
      .get(org.typelevel.ci.CIString("Accept-Language"))
      .map(_.head.value)
    languageService.resolveLanguage(explicit, acceptLang)
  }

  private val apiRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ GET -> Root / "posts" :? LimitParamMatcher(limit) +& OffsetParamMatcher(offset)
        +& TagParamMatcher(maybeTag) +& LangParamMatcher(maybeLang) =>
      withValidPagination(limit, offset) { (l, o) =>
        resolveLang(req, maybeLang).flatMap { lang =>
          maybeTag match {
            case Some(tagSlug) =>
              postService.postsByTag(lang, tagSlug, l, o).map(_.asJson).flatMap(Ok(_))
            case None =>
              postService.allPosts(lang, l, o).map(_.asJson).flatMap(Ok(_))
          }
        }
      }

    case req @ GET -> Root / "posts" / "search" :? QueryParamMatcher(query)
        +& LimitParamMatcher(limit) +& OffsetParamMatcher(offset)
        +& LangParamMatcher(maybeLang) =>
      withValidPagination(limit, offset) { (l, o) =>
        resolveLang(req, maybeLang).flatMap { lang =>
          postService.searchPosts(lang, query, l, o).map(_.asJson).flatMap(Ok(_))
        }
      }

    case req @ GET -> Root / "posts" / "recent" :? CountParamMatcher(maybeCount)
        +& LangParamMatcher(maybeLang) =>
      val count = maybeCount
        .getOrElse(DefaultRecentPostsCount)
        .min(MaxRecentPostsCount)
        .max(MinRecentPostsCount)
      resolveLang(req, maybeLang).flatMap { lang =>
        postService.recentPosts(lang, count).map(_.asJson).flatMap(Ok(_))
      }

    case req @ GET -> Root / "posts" / IntVar(id) :? LangParamMatcher(maybeLang) =>
      resolveLang(req, maybeLang).flatMap { lang =>
        postService.postById(lang, PostId(id)).map(_.asJson).flatMap(Ok(_))
      }

    case POST -> Root / "posts" / IntVar(id) / "view" =>
      postService.incrementViewCount(PostId(id)) *> NoContent()

    case GET -> Root / "posts" / IntVar(id) / "comments" =>
      commentService.getCommentsForPost(PostId(id)).map(_.asJson).flatMap(Ok(_))

    case req @ POST -> Root / "posts" / IntVar(id) / "comments" =>
      req.as[CreateCommentRequest].flatMap { request =>
        withValidComment(request) { validated =>
          commentService.createComment(PostId(id), validated).map(_.asJson).flatMap(Created(_))
        }
      }

    case req @ POST -> Root / "comments" / IntVar(id) / "rate" =>
      val ip = extractIp(req)
      req.as[RateCommentRequest].flatMap { request =>
        commentService.rateComment(CommentId(id), request.isUpvote, ip) *> NoContent()
      }

    case req @ GET -> Root / "tags" / "cloud" :? LangParamMatcher(maybeLang) =>
      resolveLang(req, maybeLang).flatMap { lang =>
        tagService.getTagCloud(lang).map(_.asJson).flatMap(Ok(_))
      }

    case req @ GET -> Root / "tags" :? LangParamMatcher(maybeLang) =>
      resolveLang(req, maybeLang).flatMap { lang =>
        tagService.getAllTags(lang).map(_.asJson).flatMap(Ok(_))
      }

    case req @ GET -> Root / "pages" :? LangParamMatcher(maybeLang) =>
      resolveLang(req, maybeLang).flatMap { lang =>
        pageService.getAllPages(lang).map(_.asJson).flatMap(Ok(_))
      }

    case req @ GET -> Root / "pages" / url :? LangParamMatcher(maybeLang) =>
      resolveLang(req, maybeLang).flatMap { lang =>
        pageService.getPageByUrl(lang, url).map(_.asJson).flatMap(Ok(_))
      }

    case GET -> Root / "skills" =>
      skillService.getSkillsByCategory.map(_.asJson).flatMap(Ok(_))

    case GET -> Root / "experiences" =>
      experienceService.getExperiences.map(_.asJson).flatMap(Ok(_))

    case GET -> Root / "social-links" =>
      socialLinkService.getSocialLinks.map(_.asJson).flatMap(Ok(_))

    case req @ POST -> Root / "contact" =>
      val ip = extractIp(req)
      req.as[CreateContactRequest].flatMap { request =>
        withValidContact(request) { validated =>
          contactService.submitContact(validated, ip).map(_.asJson).flatMap(Ok(_))
        }
      }

    case GET -> Root / "about" =>
      aboutService.getAboutPage.map(_.asJson).flatMap(Ok(_))

    case req @ GET -> Root / "feed" :? LangParamMatcher(maybeLang) =>
      resolveLang(req, maybeLang).flatMap { lang =>
        feedService.getFeed(lang).map(_.asJson).flatMap(Ok(_))
      }

    case GET -> Root / "languages" =>
      languageService.getActiveLanguages.map(_.asJson).flatMap(Ok(_))
  }

  private val systemRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "health" =>
    healthService.check.map(_.asJson).flatMap(Ok(_))
  }

  val routes: HttpRoutes[F] = Router(s"/$ApiVersion" -> apiRoutes, "/" -> systemRoutes)

  private def withValidPagination(limit: Int, offset: Int)(
    f: (Int, Int) => F[org.http4s.Response[F]]
  ): F[org.http4s.Response[F]] =
    Validation.validatePagination(limit, offset) match {
      case cats.data.Validated.Valid((l, o)) => f(l, o)
      case cats.data.Validated.Invalid(errors) =>
        Concurrent[F].raiseError(AppErr.ValidationFailed(errors.toNonEmptyList.toList.toMap))
    }

  private def withValidComment(
    request: CreateCommentRequest
  )(f: CreateCommentRequest => F[org.http4s.Response[F]]): F[org.http4s.Response[F]] =
    Validation.validateComment(request.name, request.email, request.text) match {
      case cats.data.Validated.Valid((name, email, text)) =>
        f(request.copy(name = name, email = email, text = text))
      case cats.data.Validated.Invalid(errors) =>
        Concurrent[F].raiseError(AppErr.ValidationFailed(errors.toNonEmptyList.toList.toMap))
    }

  private def withValidContact(
    request: CreateContactRequest
  )(f: CreateContactRequest => F[org.http4s.Response[F]]): F[org.http4s.Response[F]] =
    Validation.validateContact(
      request.name,
      request.email,
      request.subject,
      request.message
    ) match {
      case cats.data.Validated.Valid((name, email, subject, message)) =>
        f(request.copy(name = name, email = email, subject = subject, message = message))
      case cats.data.Validated.Invalid(errors) =>
        Concurrent[F].raiseError(AppErr.ValidationFailed(errors.toNonEmptyList.toList.toMap))
    }

  private def extractIp(req: Request[F]): String =
    req.headers
      .get[`X-Forwarded-For`]
      .flatMap(_.values.head)
      .map(_.toUriString)
      .orElse(req.remoteAddr.map(_.toUriString))
      .getOrElse("unknown")
}

object RoutesImpl {
  val ApiVersion = "v1"
  val DefaultRecentPostsCount = 5
  val MaxRecentPostsCount = 20
  val MinRecentPostsCount = 1

  def create[F[_]: Concurrent](
    postService: PostService[F],
    commentService: CommentService[F],
    tagService: TagService[F],
    pageService: PageService[F],
    healthService: HealthService[F],
    skillService: SkillService[F],
    experienceService: ExperienceService[F],
    socialLinkService: SocialLinkService[F],
    contactService: ContactService[F],
    aboutService: AboutService[F],
    feedService: FeedService[F],
    languageService: LanguageService[F]
  ): RoutesImpl[F] =
    new RoutesImpl[F](
      postService,
      commentService,
      tagService,
      pageService,
      healthService,
      skillService,
      experienceService,
      socialLinkService,
      contactService,
      aboutService,
      feedService,
      languageService
    )
}
