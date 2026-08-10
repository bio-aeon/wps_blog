package su.wps.blog.endpoints

import cats.data.{Validated, ValidatedNec}
import cats.effect.Async
import cats.syntax.applicativeError.*
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import org.http4s.HttpRoutes
import sttp.tapir.AnyEndpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.{Http4sServerInterpreter, Http4sServerOptions}
import sttp.tapir.server.model.ValuedEndpointOutput
import su.wps.blog.models.api.{
  CreateCommentRequest,
  CreateContactRequest,
  ErrorResponse,
  RateCommentRequest
}
import su.wps.blog.models.domain.{AppErr, CommentId, PostId}
import su.wps.blog.services.*
import su.wps.blog.validation.Validation

final class RoutesImpl[F[_]: Async] private (
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
) extends Routes[F] {
  import RoutesImpl._
  import TapirSupport.*

  private def attempt[A](fa: F[A]): F[Either[ErrorResponse, A]] =
    fa.attempt.map(_.left.map(ApiErrors.fromThrowable))

  private def resolveLang(explicit: Option[String], acceptLanguage: Option[String]): F[String] =
    languageService.resolveLanguage(explicit, acceptLanguage)

  private def validated[A](result: ValidatedNec[Validation.FieldError, A]): F[A] = result match {
    case Validated.Valid(a) => a.pure[F]
    case Validated.Invalid(errors) =>
      Async[F].raiseError(AppErr.ValidationFailed(errors.toNonEmptyList.toList.toMap))
  }

  private val getPosts =
    ApiEndpoints.getPosts.serverLogic { case (limit, offset, maybeTag, maybeLang, acceptLanguage) =>
      attempt {
        for {
          pagination <- validated(Validation.validatePagination(limit, offset))
          (l, o) = pagination
          lang <- resolveLang(maybeLang, acceptLanguage)
          result <- maybeTag match {
            case Some(tagSlug) => postService.postsByTag(lang, tagSlug, l, o)
            case None => postService.allPosts(lang, l, o)
          }
        } yield result
      }
    }

  private val searchPosts =
    ApiEndpoints.searchPosts.serverLogic { case (query, limit, offset, maybeLang, acceptLanguage) =>
      attempt {
        for {
          pagination <- validated(Validation.validatePagination(limit, offset))
          (l, o) = pagination
          lang <- resolveLang(maybeLang, acceptLanguage)
          result <- postService.searchPosts(lang, query, l, o)
        } yield result
      }
    }

  private val recentPosts =
    ApiEndpoints.recentPosts.serverLogic { case (maybeCount, maybeLang, acceptLanguage) =>
      val count = maybeCount
        .getOrElse(DefaultRecentPostsCount)
        .min(MaxRecentPostsCount)
        .max(MinRecentPostsCount)
      attempt {
        resolveLang(maybeLang, acceptLanguage).flatMap(postService.recentPosts(_, count))
      }
    }

  private val getPostById =
    ApiEndpoints.getPostById.serverLogic { case (id, maybeLang, acceptLanguage) =>
      attempt {
        resolveLang(maybeLang, acceptLanguage).flatMap(postService.postById(_, PostId(id)))
      }
    }

  private val incrementViewCount =
    ApiEndpoints.incrementViewCount.serverLogic { id =>
      attempt(postService.incrementViewCount(PostId(id)))
    }

  private val getCommentsForPost =
    ApiEndpoints.getCommentsForPost.serverLogic { id =>
      attempt(commentService.getCommentsForPost(PostId(id)))
    }

  private val createComment =
    ApiEndpoints.createComment.serverLogic { case (id, request) =>
      attempt {
        for {
          fields <- validated(Validation.validateComment(request.name, request.email, request.text))
          (name, email, text) = fields
          created <- commentService.createComment(
            PostId(id),
            request.copy(name = name, email = email, text = text)
          )
        } yield created
      }
    }

  private val rateComment =
    ApiEndpoints.rateComment.serverLogic { case (id, request, ip) =>
      attempt(commentService.rateComment(CommentId(id), request.isUpvote, ip))
    }

  private val getAllTags =
    ApiEndpoints.getAllTags.serverLogic { case (maybeLang, acceptLanguage) =>
      attempt(resolveLang(maybeLang, acceptLanguage).flatMap(tagService.getAllTags))
    }

  private val getTagCloud =
    ApiEndpoints.getTagCloud.serverLogic { case (maybeLang, acceptLanguage) =>
      attempt(resolveLang(maybeLang, acceptLanguage).flatMap(tagService.getTagCloud))
    }

  private val getAllPages =
    ApiEndpoints.getAllPages.serverLogic { case (maybeLang, acceptLanguage) =>
      attempt(resolveLang(maybeLang, acceptLanguage).flatMap(pageService.getAllPages))
    }

  private val getPageByUrl =
    ApiEndpoints.getPageByUrl.serverLogic { case (url, maybeLang, acceptLanguage) =>
      attempt {
        resolveLang(maybeLang, acceptLanguage).flatMap(pageService.getPageByUrl(_, url))
      }
    }

  private val healthCheck =
    ApiEndpoints.healthCheck.serverLogic(_ => attempt(healthService.check))

  private val getSkills =
    ApiEndpoints.getSkills.serverLogic(_ => attempt(skillService.getSkillsByCategory))

  private val getExperiences =
    ApiEndpoints.getExperiences.serverLogic(_ => attempt(experienceService.getExperiences))

  private val getSocialLinks =
    ApiEndpoints.getSocialLinks.serverLogic(_ => attempt(socialLinkService.getSocialLinks))

  private val submitContact =
    ApiEndpoints.submitContact.serverLogic { case (request, ip) =>
      attempt {
        for {
          fields <- validated(
            Validation
              .validateContact(request.name, request.email, request.subject, request.message)
          )
          (name, email, subject, message) = fields
          response <- contactService.submitContact(
            request.copy(name = name, email = email, subject = subject, message = message),
            ip
          )
        } yield response
      }
    }

  private val getAbout =
    ApiEndpoints.getAbout.serverLogic(_ => attempt(aboutService.getAboutPage))

  private val getFeed =
    ApiEndpoints.getFeed.serverLogic { case (maybeLang, acceptLanguage) =>
      attempt(resolveLang(maybeLang, acceptLanguage).flatMap(feedService.getFeed))
    }

  private val getLanguages =
    ApiEndpoints.getLanguages.serverLogic(_ => attempt(languageService.getActiveLanguages))

  /** Order matters: the literal `search` and `recent` paths must precede `posts/{id}`. */
  val serverEndpoints: List[ServerEndpoint[Any, F]] = List(
    getPosts,
    searchPosts,
    recentPosts,
    getPostById,
    incrementViewCount,
    getCommentsForPost,
    createComment,
    rateComment,
    getTagCloud,
    getAllTags,
    getAllPages,
    getPageByUrl,
    healthCheck,
    getSkills,
    getExperiences,
    getSocialLinks,
    submitContact,
    getAbout,
    getFeed,
    getLanguages
  )

  val endpoints: List[AnyEndpoint] = serverEndpoints.map(_.endpoint)

  /** Renders tapir's own decode failures as ErrorResponse, so malformed requests use the same JSON
    * envelope as every other error.
    */
  private val serverOptions: Http4sServerOptions[F] =
    Http4sServerOptions
      .customiseInterceptors[F]
      .defaultHandlers(message =>
        ValuedEndpointOutput(jsonBody[ErrorResponse], ErrorResponse.badRequest(message))
      )
      .options

  val routes: HttpRoutes[F] =
    Http4sServerInterpreter[F](serverOptions).toRoutes(serverEndpoints)
}

object RoutesImpl {
  val ApiVersion = "v1"
  val DefaultRecentPostsCount = 5
  val MaxRecentPostsCount = 20
  val MinRecentPostsCount = 1

  def create[F[_]: Async](
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
