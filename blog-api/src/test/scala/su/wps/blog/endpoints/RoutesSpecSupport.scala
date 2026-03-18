package su.wps.blog.endpoints

import cats.effect.Concurrent
import su.wps.blog.endpoints.mocks.*
import su.wps.blog.models.api.*
import su.wps.blog.models.domain.{AppErr, CommentId, PostId, TagId}
import tofu.Raise

import java.time.ZonedDateTime

trait RoutesSpecSupport {

  protected val testTimestamp = ZonedDateTime.parse("2001-01-01T09:15:00Z")
  protected val v1 = RoutesImpl.ApiVersion

  protected val testTags = List(TagResult(TagId(1), "scala", "scala"))

  protected val testLang = "en"
  protected val testAvailableLangs = List("en")

  protected val testPostList = List(
    ListPostResult(
      PostId(1),
      "name",
      Some("text"),
      testTimestamp,
      testLang,
      testTags,
      testAvailableLangs
    )
  )

  protected val testSinglePost =
    PostResult(PostId(1), "name", "text", testTimestamp, testLang, testTags, None, testAvailableLangs)

  protected val testTaggedPosts = List(
    ListPostResult(
      PostId(2),
      "scala-post",
      Some("scala-text"),
      testTimestamp,
      testLang,
      testTags,
      testAvailableLangs
    )
  )

  protected val testSearchResults = List(
    ListPostResult(
      PostId(1),
      "scala-tutorial",
      Some("Learn Scala programming"),
      testTimestamp,
      testLang,
      testTags,
      testAvailableLangs
    ),
    ListPostResult(
      PostId(2),
      "scala-advanced",
      Some("Advanced Scala topics"),
      ZonedDateTime.parse("2001-01-02T09:15:00Z"),
      testLang,
      testTags,
      testAvailableLangs
    )
  )

  protected val testRecentPosts = List(
    ListPostResult(
      PostId(1),
      "recent-post",
      Some("Recent post text"),
      testTimestamp,
      testLang,
      testTags,
      testAvailableLangs
    ),
    ListPostResult(
      PostId(2),
      "another-recent",
      Some("Another recent post"),
      ZonedDateTime.parse("2001-01-02T09:15:00Z"),
      testLang,
      testTags,
      testAvailableLangs
    )
  )

  protected val testCommentsResult = {
    val reply = CommentResult(
      CommentId(2),
      "Replier",
      "Reply text",
      1,
      ZonedDateTime.parse("2001-01-01T10:00:00Z"),
      Nil
    )
    val root = CommentResult(
      CommentId(1),
      "Author",
      "Root comment",
      5,
      ZonedDateTime.parse("2001-01-01T09:00:00Z"),
      List(reply)
    )
    CommentsListResult(List(root), 2)
  }

  protected val testCreatedComment = CommentResult(
    CommentId(1),
    "Author",
    "Comment text",
    0,
    ZonedDateTime.parse("2001-01-01T09:00:00Z"),
    Nil
  )

  protected val testTagsWithCounts = List(
    TagWithCountResult(TagId(1), "scala", "scala", 10),
    TagWithCountResult(TagId(2), "rust", "rust", 5)
  )

  protected val testTagCloud = TagCloudResult(
    List(TagCloudItem("scala", "scala", 10, 1.0), TagCloudItem("rust", "rust", 5, 0.5))
  )

  protected val testPage = PageResult(
    1,
    "about",
    "About Us",
    Some("About page content"),
    testTimestamp,
    testLang,
    None,
    testAvailableLangs
  )

  protected val testPagesList = ListItemsResult(
    List(
      ListPageResult("about", "About Us", testLang),
      ListPageResult("contact", "Contact", testLang)
    ),
    2
  )

  protected val testSkillCategories = List(
    SkillCategoryResult(
      "Backend",
      List(SkillResult(su.wps.blog.models.domain.SkillId(1), "Scala", "scala", "Backend", 90, None))
    )
  )

  protected val testExperiences = List(
    ExperienceResult(
      su.wps.blog.models.domain.ExperienceId(1),
      "Acme Corp",
      "Engineer",
      "Description",
      java.time.LocalDate.of(2020, 1, 1),
      None,
      Some("Remote"),
      None
    )
  )

  protected val testSocialLinks = List(
    SocialLinkResult(
      su.wps.blog.models.domain.SocialLinkId(1),
      "github",
      "https://github.com/user",
      Some("GitHub"),
      None
    )
  )

  protected val testAbout = AboutResult(
    ProfileResult("John", "Engineer", "/photo.jpg", "/resume.pdf", "Bio text"),
    testSkillCategories,
    List(
      ExperienceResult(
        su.wps.blog.models.domain.ExperienceId(1),
        "Acme",
        "Dev",
        "Desc",
        java.time.LocalDate.of(2020, 1, 1),
        None,
        None,
        None
      )
    ),
    List(
      SocialLinkResult(
        su.wps.blog.models.domain.SocialLinkId(1),
        "github",
        "https://github.com",
        Some("GitHub"),
        None
      )
    )
  )

  protected def buildRoutes[F[_]: Concurrent: Raise[*[_], AppErr]](
    allPostsResult: List[ListPostResult] = Nil,
    postByIdResult: Option[PostResult] = None,
    postsByTagResult: List[ListPostResult] = Nil,
    searchPostsResult: List[ListPostResult] = Nil,
    recentPostsResult: List[ListPostResult] = Nil,
    commentsResult: CommentsListResult = CommentsListResult(Nil, 0),
    createCommentResult: Option[CommentResult] = None,
    tagsResult: List[TagWithCountResult] = Nil,
    tagCloudResult: TagCloudResult = TagCloudResult(Nil),
    pageResult: Option[PageResult] = None,
    pagesResult: ListItemsResult[ListPageResult] = ListItemsResult(Nil, 0),
    healthStatus: String = "healthy",
    healthDatabase: String = "healthy",
    skillsResult: List[SkillCategoryResult] = Nil,
    experiencesResult: List[ExperienceResult] = Nil,
    socialLinksResult: List[SocialLinkResult] = Nil,
    aboutResult: AboutResult = AboutResult(ProfileResult("", "", "", "", ""), Nil, Nil, Nil),
    feedResult: FeedResult = FeedResult(Nil, Nil, Nil)
  ): Routes[F] = RoutesImpl.create[F](
    PostServiceMock.create[F](
      allPostsResult,
      postByIdResult,
      postsByTagResult,
      searchPostsResult,
      recentPostsResult = recentPostsResult
    ),
    CommentServiceMock.create[F](commentsResult, createCommentResult),
    TagServiceMock.create[F](tagsResult, tagCloudResult),
    PageServiceMock.create[F](pageResult, pagesResult),
    HealthServiceMock.create[F](healthStatus, healthDatabase, testTimestamp),
    SkillServiceMock.create[F](skillsResult),
    ExperienceServiceMock.create[F](experiencesResult),
    SocialLinkServiceMock.create[F](socialLinksResult),
    ContactServiceMock.create[F](),
    AboutServiceMock.create[F](aboutResult),
    FeedServiceMock.create[F](feedResult),
    LanguageServiceMock.create[F]()
  )

}
