package su.wps.blog.services

import cats.Id
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless.*
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.AppErr.PostNotFound
import su.wps.blog.models.domain.{Post, PostId, Tag, TagId}
import su.wps.blog.services.mocks.*
import su.wps.blog.tools.scalacheck.*
import tofu.doobie.transactor.Txr

class PostServiceSpec extends Specification {
  type RunF[A] = Either[Throwable, A]

  val xa: Txr[RunF, Id] = TxrMock.create[RunF]

  implicit val genPost: Gen[Post] = for {
    id <- arbitrary[PostId]
    post <- arbitrary[Post].map(_.copy(id = Some(id)))
  } yield post

  implicit val genTag: Gen[Tag] = for {
    id <- arbitrary[TagId]
    tag <- arbitrary[Tag].map(_.copy(id = Some(id)))
  } yield tag

  private def postsWithUniqueIds(count: Int): List[Post] =
    random[Post](count).zipWithIndex.map { case (p, i) => p.copy(id = Some(PostId(i + 1))) }

  "PostService" >> {
    "returns posts with total count by limit and offset" >> {
      val service = mkService(random[Post](5), 10)

      service.allPosts("en", 5, 10) must beRight.which(r => r.items.length == 5 && r.total == 10)
    }

    "includes tags in post list results" >> {
      val posts = postsWithUniqueIds(2)
      val tag1 = Tag("scala", "scala", Some(TagId(1)))
      val tag2 = Tag("rust", "rust", Some(TagId(2)))
      val tagsByPost = posts.flatMap(p => p.id.map(id => List((id, tag1), (id, tag2)))).flatten
      val service = mkService(posts, 2, findByPostIdsResult = tagsByPost)

      service.allPosts("en", 10, 0) must beRight.which { r =>
        r.items.forall(_.tags.length == 2) && r.items.forall(_.tags.exists(_.name == "scala"))
      }
    }

    "returns post if exists" >> {
      val post = random[Post]
      val service = mkService(findByIdResult = Some(post))

      service.postById("en", post.nonEmptyId) must beRight.which(_.name == post.name)
    }

    "includes tags in single post result" >> {
      val post = random[Post]
      val tags = List(Tag("scala", "scala", Some(TagId(1))), Tag("fp", "fp", Some(TagId(2))))
      val service = mkService(findByIdResult = Some(post), findByPostIdResult = tags)

      service.postById("en", post.nonEmptyId) must beRight.which { r =>
        r.tags.length == 2 && r.tags.exists(_.name == "scala")
      }
    }

    "returns an error if post doesn't exist" >> {
      val service = mkService()

      service.postById("en", PostId(1)) must beLeft(PostNotFound(PostId(1)))
    }

    "handles posts with no tags" >> {
      val posts = random[Post](2)
      val service = mkService(posts, 2, findByPostIdsResult = Nil)

      service.allPosts("en", 10, 0) must beRight.which { r =>
        r.items.forall(_.tags.isEmpty)
      }
    }

    "returns posts filtered by tag with correct pagination" >> {
      val posts = random[Post](3)
      val service = mkService(findByTagSlugResult = posts, findCountByTagSlugResult = 10)

      service.postsByTag("en", "scala", 3, 0) must beRight.which { r =>
        r.items.length == 3 && r.total == 10
      }
    }

    "includes tags in filtered post results" >> {
      val posts = postsWithUniqueIds(2)
      val tag1 = Tag("scala", "scala", Some(TagId(1)))
      val tag2 = Tag("fp", "fp", Some(TagId(2)))
      val tagsByPost = posts.flatMap(p => p.id.map(id => List((id, tag1), (id, tag2)))).flatten
      val service = mkService(
        findByTagSlugResult = posts,
        findCountByTagSlugResult = 2,
        findByPostIdsResult = tagsByPost
      )

      service.postsByTag("en", "scala", 10, 0) must beRight.which { r =>
        r.items.forall(_.tags.length == 2) && r.items.forall(_.tags.exists(_.slug == "scala"))
      }
    }

    "returns empty result for non-existent tag" >> {
      val service = mkService(findByTagSlugResult = Nil, findCountByTagSlugResult = 0)

      service.postsByTag("en", "nonexistent", 10, 0) must beRight.which { r =>
        r.items.isEmpty && r.total == 0
      }
    }

    "successfully increments view count for visible post" >> {
      val service = mkService(incrementViewsResult = 1)

      service.incrementViewCount(PostId(1)) must beRight(())
    }

    "completes without error for hidden post (no-op)" >> {
      val service = mkService(incrementViewsResult = 0)

      service.incrementViewCount(PostId(1)) must beRight(())
    }

    "returns search results with correct pagination" >> {
      val posts = random[Post](3)
      val service = mkService(searchPostsResult = posts, searchPostsCountResult = 10)

      service.searchPosts("en", "scala", 3, 0) must beRight.which { r =>
        r.items.length == 3 && r.total == 10
      }
    }

    "includes tags in search results" >> {
      val posts = postsWithUniqueIds(2)
      val tag1 = Tag("scala", "scala", Some(TagId(1)))
      val tag2 = Tag("fp", "fp", Some(TagId(2)))
      val tagsByPost = posts.flatMap(p => p.id.map(id => List((id, tag1), (id, tag2)))).flatten
      val service = mkService(
        searchPostsResult = posts,
        searchPostsCountResult = 2,
        findByPostIdsResult = tagsByPost
      )

      service.searchPosts("en", "scala", 10, 0) must beRight.which { r =>
        r.items.forall(_.tags.length == 2) && r.items.forall(_.tags.exists(_.name == "scala"))
      }
    }

    "returns empty result for search with no matches" >> {
      val service = mkService(searchPostsResult = Nil, searchPostsCountResult = 0)

      service.searchPosts("en", "nonexistent", 10, 0) must beRight.which { r =>
        r.items.isEmpty && r.total == 0
      }
    }

    "returns recent posts limited by count" >> {
      val posts = random[Post](5)
      val service = mkService(findRecentResult = posts)

      service.recentPosts("en", 5) must beRight.which(_.length == 5)
    }

    "includes tags in recent posts results" >> {
      val posts = postsWithUniqueIds(2)
      val tag1 = Tag("scala", "scala", Some(TagId(1)))
      val tag2 = Tag("fp", "fp", Some(TagId(2)))
      val tagsByPost = posts.flatMap(p => p.id.map(id => List((id, tag1), (id, tag2)))).flatten
      val service = mkService(findRecentResult = posts, findByPostIdsResult = tagsByPost)

      service.recentPosts("en", 5) must beRight.which { r =>
        r.forall(_.tags.length == 2) && r.forall(_.tags.exists(_.name == "scala"))
      }
    }

    "returns empty list when no recent posts exist" >> {
      val service = mkService(findRecentResult = Nil)

      service.recentPosts("en", 5) must beRight.which(_.isEmpty)
    }

    "maps non-empty SEO fields to Some" >> {
      val post = random[Post].copy(
        metaTitle = "Title",
        metaDescription = "Description",
        metaKeywords = "kw1, kw2"
      )
      val service = mkService(findByIdResult = Some(post))

      service.postById("en", post.nonEmptyId) must beRight.which { r =>
        r.seo.exists(s =>
          s.title.contains("Title") &&
          s.description.contains("Description") &&
          s.keywords.contains("kw1, kw2")
        )
      }
    }

    "maps empty SEO fields to None" >> {
      val post = random[Post].copy(metaTitle = "", metaDescription = "", metaKeywords = "")
      val service = mkService(findByIdResult = Some(post))

      service.postById("en", post.nonEmptyId) must beRight.which { r =>
        r.seo.isEmpty
      }
    }

    "maps whitespace-only SEO fields to None" >> {
      val post = random[Post].copy(metaTitle = "  ", metaDescription = " \t ", metaKeywords = "  ")
      val service = mkService(findByIdResult = Some(post))

      service.postById("en", post.nonEmptyId) must beRight.which { r =>
        r.seo.isEmpty
      }
    }

    "handles recent posts with no tags" >> {
      val posts = random[Post](3)
      val service = mkService(findRecentResult = posts, findByPostIdsResult = Nil)

      service.recentPosts("en", 5) must beRight.which { r =>
        r.length == 3 && r.forall(_.tags.isEmpty)
      }
    }
  }

  private def mkService(
    findAllResult: List[Post] = Nil,
    findCountResult: Int = 0,
    findByIdResult: Option[Post] = None,
    findByPostIdResult: List[Tag] = Nil,
    findByPostIdsResult: List[(PostId, Tag)] = Nil,
    findByTagSlugResult: List[Post] = Nil,
    findCountByTagSlugResult: Int = 0,
    incrementViewsResult: Int = 1,
    searchPostsResult: List[Post] = Nil,
    searchPostsCountResult: Int = 0,
    findRecentResult: List[Post] = Nil
  ): PostService[RunF] = {
    val postRepo = PostRepositoryMock.create[Id](
      findAllResult,
      findCountResult,
      findByIdResult,
      findByTagSlugResult = findByTagSlugResult,
      findCountByTagSlugResult = findCountByTagSlugResult,
      incrementViewsResult = incrementViewsResult,
      searchPostsResult = searchPostsResult,
      searchPostsCountResult = searchPostsCountResult,
      findRecentResult = findRecentResult
    )
    val tagRepo = TagRepositoryMock.create[Id](
      findByPostIdResult = findByPostIdResult,
      findByPostIdsResult = findByPostIdsResult
    )
    val postTranslationRepo = PostTranslationRepositoryMock.create[Id]()
    val tagTranslationRepo = TagTranslationRepositoryMock.create[Id]()
    PostServiceImpl.create[RunF, Id](postRepo, tagRepo, postTranslationRepo, tagTranslationRepo, xa)
  }
}
