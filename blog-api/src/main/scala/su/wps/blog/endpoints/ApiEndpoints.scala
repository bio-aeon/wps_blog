package su.wps.blog.endpoints

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import su.wps.blog.models.api.*

object ApiEndpoints {
  import TapirSupport.*

  private val v1 = RoutesImpl.ApiVersion
  private val postsTag = "Posts"
  private val commentsTag = "Comments"
  private val tagsTag = "Tags"
  private val pagesTag = "Pages"
  private val systemTag = "System"
  private val profileTag = "Profile"
  private val contactTag = "Contact"
  private val feedTag = "Feed"
  private val languagesTag = "Languages"

  private val langQuery =
    query[Option[String]]("lang").description("Language code (en, ru, el)")

  private val acceptLanguage =
    header[Option[String]]("Accept-Language")
      .description("Fallback language when the lang query parameter is absent")

  /** Extracted rather than declared as an input: not part of the documented request contract. */
  private val clientIp: EndpointInput[String] =
    extractFromRequest[String] { req =>
      req
        .header("X-Forwarded-For")
        .flatMap(_.split(",").headOption)
        .map(_.trim)
        .filter(_.nonEmpty)
        .orElse(req.connectionInfo.remote.map(_.getAddress.getHostAddress))
        .getOrElse("unknown")
    }

  val getPosts =
    endpoint.get
      .in(v1 / "posts")
      .in(query[Int]("limit").description("Number of posts per page"))
      .in(query[Int]("offset").description("Pagination offset"))
      .in(
        query[Option[String]]("tag")
          .description("Filter by tag slug. When provided, returns only posts with this tag")
      )
      .in(langQuery)
      .in(acceptLanguage)
      .out(jsonBody[ListItemsResult[ListPostResult]])
      .errorOut(ApiErrors.output)
      .summary("List posts")
      .description("List blog posts with pagination. Optionally filter by tag slug.")
      .tag(postsTag)

  val searchPosts =
    endpoint.get
      .in(v1 / "posts" / "search")
      .in(query[String]("q").description("Full-text search query"))
      .in(query[Int]("limit").description("Number of results per page"))
      .in(query[Int]("offset").description("Pagination offset"))
      .in(langQuery)
      .in(acceptLanguage)
      .out(jsonBody[ListItemsResult[ListPostResult]])
      .errorOut(ApiErrors.output)
      .summary("Search posts")
      .description("Full-text search across blog post titles and content.")
      .tag(postsTag)

  val recentPosts =
    endpoint.get
      .in(v1 / "posts" / "recent")
      .in(
        query[Option[Int]]("count")
          .description("Number of recent posts to return (default 5, max 20)")
      )
      .in(langQuery)
      .in(acceptLanguage)
      .out(jsonBody[List[ListPostResult]])
      .errorOut(ApiErrors.output)
      .summary("Recent posts")
      .description("Get the most recent blog posts.")
      .tag(postsTag)

  val getPostById =
    endpoint.get
      .in(v1 / "posts" / path[Int]("id").description("Post ID"))
      .in(langQuery)
      .in(acceptLanguage)
      .out(jsonBody[PostResult])
      .errorOut(ApiErrors.output)
      .summary("Get post by ID")
      .description("Get a single blog post by its ID.")
      .tag(postsTag)

  val incrementViewCount =
    endpoint.post
      .in(v1 / "posts" / path[Int]("id").description("Post ID") / "view")
      .out(statusCode(StatusCode.NoContent))
      .errorOut(ApiErrors.output)
      .summary("Increment view count")
      .description(
        "Increment the view counter for a post. " +
          "Idempotent - returns 204 even for non-existent posts."
      )
      .tag(postsTag)

  val getCommentsForPost =
    endpoint.get
      .in(v1 / "posts" / path[Int]("id").description("Post ID") / "comments")
      .out(jsonBody[CommentsListResult])
      .errorOut(ApiErrors.output)
      .summary("List comments for post")
      .description("Get all comments for a post as a threaded tree structure.")
      .tag(commentsTag)

  val createComment =
    endpoint.post
      .in(v1 / "posts" / path[Int]("id").description("Post ID") / "comments")
      .in(jsonBody[CreateCommentRequest])
      .out(statusCode(StatusCode.Created).and(jsonBody[CommentResult]))
      .errorOut(ApiErrors.output)
      .summary("Create comment")
      .description(
        "Create a new comment on a post. " +
          "Set parentId to reply to an existing comment."
      )
      .tag(commentsTag)

  val rateComment =
    endpoint.post
      .in(v1 / "comments" / path[Int]("id").description("Comment ID") / "rate")
      .in(jsonBody[RateCommentRequest])
      .in(clientIp)
      .out(statusCode(StatusCode.NoContent))
      .errorOut(ApiErrors.output)
      .summary("Rate comment")
      .description(
        "Upvote or downvote a comment. " +
          "Each IP address can only rate once per comment."
      )
      .tag(commentsTag)

  val getAllTags =
    endpoint.get
      .in(v1 / "tags")
      .in(langQuery)
      .in(acceptLanguage)
      .out(jsonBody[ListItemsResult[TagWithCountResult]])
      .errorOut(ApiErrors.output)
      .summary("List all tags")
      .description("Get all tags with their associated post counts.")
      .tag(tagsTag)

  val getTagCloud =
    endpoint.get
      .in(v1 / "tags" / "cloud")
      .in(langQuery)
      .in(acceptLanguage)
      .out(jsonBody[TagCloudResult])
      .errorOut(ApiErrors.output)
      .summary("Tag cloud")
      .description("Get tag cloud data with normalized weights for visualization.")
      .tag(tagsTag)

  val getAllPages =
    endpoint.get
      .in(v1 / "pages")
      .in(langQuery)
      .in(acceptLanguage)
      .out(jsonBody[ListItemsResult[ListPageResult]])
      .errorOut(ApiErrors.output)
      .summary("List all pages")
      .description("Get all static pages (URL and title) for navigation menus.")
      .tag(pagesTag)

  val getPageByUrl =
    endpoint.get
      .in(v1 / "pages" / path[String]("url").description("Page URL slug"))
      .in(langQuery)
      .in(acceptLanguage)
      .out(jsonBody[PageResult])
      .errorOut(ApiErrors.output)
      .summary("Get page by URL")
      .description("Get a static page by its URL slug.")
      .tag(pagesTag)

  val healthCheck =
    endpoint.get
      .in("health")
      .out(jsonBody[HealthResponse])
      .errorOut(ApiErrors.output)
      .summary("Health check")
      .description("Check API and database health status.")
      .tag(systemTag)

  val getSkills =
    endpoint.get
      .in(v1 / "skills")
      .out(jsonBody[List[SkillCategoryResult]])
      .errorOut(ApiErrors.output)
      .summary("List skills by category")
      .description("Get all active skills grouped by category with proficiency levels.")
      .tag(profileTag)

  val getExperiences =
    endpoint.get
      .in(v1 / "experiences")
      .out(jsonBody[List[ExperienceResult]])
      .errorOut(ApiErrors.output)
      .summary("List experiences")
      .description("Get all active work experiences ordered chronologically.")
      .tag(profileTag)

  val getSocialLinks =
    endpoint.get
      .in(v1 / "social-links")
      .out(jsonBody[List[SocialLinkResult]])
      .errorOut(ApiErrors.output)
      .summary("List social links")
      .description("Get all active social/platform links.")
      .tag(profileTag)

  val submitContact =
    endpoint.post
      .in(v1 / "contact")
      .in(jsonBody[CreateContactRequest])
      .in(clientIp)
      .out(jsonBody[ContactResponse])
      .errorOut(ApiErrors.output)
      .summary("Submit contact form")
      .description("Submit a contact form message. Rate limited per IP address.")
      .tag(contactTag)

  val getAbout =
    endpoint.get
      .in(v1 / "about")
      .out(jsonBody[AboutResult])
      .errorOut(ApiErrors.output)
      .summary("Get about page")
      .description("Get aggregated about page data: profile, skills, experiences, social links.")
      .tag(profileTag)

  val getFeed =
    endpoint.get
      .in(v1 / "feed")
      .in(langQuery)
      .in(acceptLanguage)
      .out(jsonBody[FeedResult])
      .errorOut(ApiErrors.output)
      .summary("Get feed data")
      .description("Get all posts, pages, and tags for sitemap/RSS/feed generation.")
      .tag(feedTag)

  val getLanguages =
    endpoint.get
      .in(v1 / "languages")
      .out(jsonBody[List[LanguageResult]])
      .errorOut(ApiErrors.output)
      .summary("List active languages")
      .description("Get all active languages with their native names.")
      .tag(languagesTag)
}
