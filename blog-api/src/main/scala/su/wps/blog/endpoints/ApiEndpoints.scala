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

  val getPosts: AnyEndpoint =
    endpoint.get
      .in(v1 / "posts")
      .in(query[Int]("limit").description("Number of posts per page"))
      .in(query[Int]("offset").description("Pagination offset"))
      .in(
        query[Option[String]]("tag")
          .description("Filter by tag slug. When provided, returns only posts with this tag")
      )
      .out(jsonBody[ListItemsResult[ListPostResult]])
      .errorOut(jsonBody[ErrorResponse])
      .summary("List posts")
      .description("List blog posts with pagination. Optionally filter by tag slug.")
      .tag(postsTag)

  val searchPosts: AnyEndpoint =
    endpoint.get
      .in(v1 / "posts" / "search")
      .in(query[String]("q").description("Full-text search query"))
      .in(query[Int]("limit").description("Number of results per page"))
      .in(query[Int]("offset").description("Pagination offset"))
      .out(jsonBody[ListItemsResult[ListPostResult]])
      .errorOut(jsonBody[ErrorResponse])
      .summary("Search posts")
      .description("Full-text search across blog post titles and content.")
      .tag(postsTag)

  val recentPosts: AnyEndpoint =
    endpoint.get
      .in(v1 / "posts" / "recent")
      .in(
        query[Option[Int]]("count")
          .description("Number of recent posts to return (default 5, max 20)")
      )
      .out(jsonBody[List[ListPostResult]])
      .summary("Recent posts")
      .description("Get the most recent blog posts.")
      .tag(postsTag)

  val getPostById: AnyEndpoint =
    endpoint.get
      .in(v1 / "posts" / path[Int]("id").description("Post ID"))
      .out(jsonBody[PostResult])
      .errorOut(jsonBody[ErrorResponse])
      .summary("Get post by ID")
      .description("Get a single blog post by its ID.")
      .tag(postsTag)

  val incrementViewCount: AnyEndpoint =
    endpoint.post
      .in(v1 / "posts" / path[Int]("id").description("Post ID") / "view")
      .out(statusCode(StatusCode.NoContent))
      .summary("Increment view count")
      .description(
        "Increment the view counter for a post. " +
          "Idempotent - returns 204 even for non-existent posts."
      )
      .tag(postsTag)

  val getCommentsForPost: AnyEndpoint =
    endpoint.get
      .in(v1 / "posts" / path[Int]("id").description("Post ID") / "comments")
      .out(jsonBody[CommentsListResult])
      .summary("List comments for post")
      .description("Get all comments for a post as a threaded tree structure.")
      .tag(commentsTag)

  val createComment: AnyEndpoint =
    endpoint.post
      .in(v1 / "posts" / path[Int]("id").description("Post ID") / "comments")
      .in(jsonBody[CreateCommentRequest])
      .out(statusCode(StatusCode.Created).and(jsonBody[CommentResult]))
      .errorOut(jsonBody[ErrorResponse])
      .summary("Create comment")
      .description(
        "Create a new comment on a post. " +
          "Set parentId to reply to an existing comment."
      )
      .tag(commentsTag)

  val rateComment: AnyEndpoint =
    endpoint.post
      .in(v1 / "comments" / path[Int]("id").description("Comment ID") / "rate")
      .in(jsonBody[RateCommentRequest])
      .out(statusCode(StatusCode.NoContent))
      .summary("Rate comment")
      .description(
        "Upvote or downvote a comment. " +
          "Each IP address can only rate once per comment."
      )
      .tag(commentsTag)

  val getAllTags: AnyEndpoint =
    endpoint.get
      .in(v1 / "tags")
      .out(jsonBody[ListItemsResult[TagWithCountResult]])
      .summary("List all tags")
      .description("Get all tags with their associated post counts.")
      .tag(tagsTag)

  val getTagCloud: AnyEndpoint =
    endpoint.get
      .in(v1 / "tags" / "cloud")
      .out(jsonBody[TagCloudResult])
      .summary("Tag cloud")
      .description("Get tag cloud data with normalized weights for visualization.")
      .tag(tagsTag)

  val getAllPages: AnyEndpoint =
    endpoint.get
      .in(v1 / "pages")
      .out(jsonBody[ListItemsResult[ListPageResult]])
      .summary("List all pages")
      .description("Get all static pages (URL and title) for navigation menus.")
      .tag(pagesTag)

  val getPageByUrl: AnyEndpoint =
    endpoint.get
      .in(v1 / "pages" / path[String]("url").description("Page URL slug"))
      .out(jsonBody[PageResult])
      .errorOut(jsonBody[ErrorResponse])
      .summary("Get page by URL")
      .description("Get a static page by its URL slug.")
      .tag(pagesTag)

  val healthCheck: AnyEndpoint =
    endpoint.get
      .in("health")
      .out(jsonBody[HealthResponse])
      .summary("Health check")
      .description("Check API and database health status.")
      .tag(systemTag)

  val getSkills: AnyEndpoint =
    endpoint.get
      .in(v1 / "skills")
      .out(jsonBody[List[SkillCategoryResult]])
      .summary("List skills by category")
      .description("Get all active skills grouped by category with proficiency levels.")
      .tag(profileTag)

  val getExperiences: AnyEndpoint =
    endpoint.get
      .in(v1 / "experiences")
      .out(jsonBody[List[ExperienceResult]])
      .summary("List experiences")
      .description("Get all active work experiences ordered chronologically.")
      .tag(profileTag)

  val getSocialLinks: AnyEndpoint =
    endpoint.get
      .in(v1 / "social-links")
      .out(jsonBody[List[SocialLinkResult]])
      .summary("List social links")
      .description("Get all active social/platform links.")
      .tag(profileTag)

  val submitContact: AnyEndpoint =
    endpoint.post
      .in(v1 / "contact")
      .in(jsonBody[CreateContactRequest])
      .out(jsonBody[ContactResponse])
      .errorOut(jsonBody[ErrorResponse])
      .summary("Submit contact form")
      .description("Submit a contact form message. Rate limited per IP address.")
      .tag(contactTag)

  val getAbout: AnyEndpoint =
    endpoint.get
      .in(v1 / "about")
      .out(jsonBody[AboutResult])
      .summary("Get about page")
      .description("Get aggregated about page data: profile, skills, experiences, social links.")
      .tag(profileTag)

  val getFeed: AnyEndpoint =
    endpoint.get
      .in(v1 / "feed")
      .out(jsonBody[FeedResult])
      .summary("Get feed data")
      .description("Get all posts, pages, and tags for sitemap/RSS/feed generation.")
      .tag(feedTag)

  val all: List[AnyEndpoint] = List(
    getPosts,
    searchPosts,
    recentPosts,
    getPostById,
    incrementViewCount,
    getCommentsForPost,
    createComment,
    rateComment,
    getAllTags,
    getTagCloud,
    getAllPages,
    getPageByUrl,
    healthCheck,
    getSkills,
    getExperiences,
    getSocialLinks,
    submitContact,
    getAbout,
    getFeed
  )
}
