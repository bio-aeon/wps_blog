package su.wps.blog.services

import cats.Id
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless.*
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.AppErr.PostNotFound
import su.wps.blog.models.domain.{Post, PostId, Tag, TagId}
import su.wps.blog.services.mocks.{PostRepositoryMock, TagRepositoryMock, TxrMock}
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

  "PostService should" >> {
    "return posts with total count by limit and offset" >> {
      val service = mkService(random[Post](5), 10)

      service.allPosts(5, 10) must beRight.which(r => r.items.length == 5 && r.total == 10)
    }

    "include tags in post list results" >> {
      val posts = random[Post](2)
      val tag1 = Tag("scala", "scala", Some(TagId(1)))
      val tag2 = Tag("rust", "rust", Some(TagId(2)))
      val tagsByPost = posts.flatMap(p => p.id.map(id => List((id, tag1), (id, tag2)))).flatten
      val service = mkService(posts, 2, findByPostIdsResult = tagsByPost)

      service.allPosts(10, 0) must beRight.which { r =>
        r.items.forall(_.tags.length == 2) && r.items.forall(_.tags.exists(_.name == "scala"))
      }
    }

    "return post if exists" >> {
      val post = random[Post]
      val service = mkService(findByIdResult = Some(post))

      service.postById(post.nonEmptyId) must beRight.which(_.name == post.name)
    }

    "include tags in single post result" >> {
      val post = random[Post]
      val tags = List(Tag("scala", "scala", Some(TagId(1))), Tag("fp", "fp", Some(TagId(2))))
      val service = mkService(findByIdResult = Some(post), findByPostIdResult = tags)

      service.postById(post.nonEmptyId) must beRight.which { r =>
        r.tags.length == 2 && r.tags.exists(_.name == "scala")
      }
    }

    "return an error if post doesn't exist" >> {
      val service = mkService()

      service.postById(PostId(1)) must beLeft(PostNotFound(PostId(1)))
    }

    "handle posts with no tags" >> {
      val posts = random[Post](2)
      val service = mkService(posts, 2, findByPostIdsResult = Nil)

      service.allPosts(10, 0) must beRight.which { r =>
        r.items.forall(_.tags.isEmpty)
      }
    }

    "return posts filtered by tag with correct pagination" >> {
      val posts = random[Post](3)
      val service = mkService(findByTagSlugResult = posts, findCountByTagSlugResult = 10)

      service.postsByTag("scala", 3, 0) must beRight.which { r =>
        r.items.length == 3 && r.total == 10
      }
    }

    "include tags in filtered post results" >> {
      val posts = random[Post](2)
      val tag1 = Tag("scala", "scala", Some(TagId(1)))
      val tag2 = Tag("fp", "fp", Some(TagId(2)))
      val tagsByPost = posts.flatMap(p => p.id.map(id => List((id, tag1), (id, tag2)))).flatten
      val service = mkService(
        findByTagSlugResult = posts,
        findCountByTagSlugResult = 2,
        findByPostIdsResult = tagsByPost
      )

      service.postsByTag("scala", 10, 0) must beRight.which { r =>
        r.items.forall(_.tags.length == 2) && r.items.forall(_.tags.exists(_.slug == "scala"))
      }
    }

    "return empty result for non-existent tag" >> {
      val service = mkService(findByTagSlugResult = Nil, findCountByTagSlugResult = 0)

      service.postsByTag("nonexistent", 10, 0) must beRight.which { r =>
        r.items.isEmpty && r.total == 0
      }
    }

    "successfully increment view count for visible post" >> {
      val service = mkService(incrementViewsResult = 1)

      service.incrementViewCount(PostId(1)) must beRight(())
    }

    "complete without error for hidden post (no-op)" >> {
      val service = mkService(incrementViewsResult = 0)

      service.incrementViewCount(PostId(1)) must beRight(())
    }

    "return search results with correct pagination" >> {
      val posts = random[Post](3)
      val service = mkService(searchPostsResult = posts, searchPostsCountResult = 10)

      service.searchPosts("scala", 3, 0) must beRight.which { r =>
        r.items.length == 3 && r.total == 10
      }
    }

    "include tags in search results" >> {
      val posts = random[Post](2)
      val tag1 = Tag("scala", "scala", Some(TagId(1)))
      val tag2 = Tag("fp", "fp", Some(TagId(2)))
      val tagsByPost = posts.flatMap(p => p.id.map(id => List((id, tag1), (id, tag2)))).flatten
      val service = mkService(
        searchPostsResult = posts,
        searchPostsCountResult = 2,
        findByPostIdsResult = tagsByPost
      )

      service.searchPosts("scala", 10, 0) must beRight.which { r =>
        r.items.forall(_.tags.length == 2) && r.items.forall(_.tags.exists(_.name == "scala"))
      }
    }

    "return empty result for search with no matches" >> {
      val service = mkService(searchPostsResult = Nil, searchPostsCountResult = 0)

      service.searchPosts("nonexistent", 10, 0) must beRight.which { r =>
        r.items.isEmpty && r.total == 0
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
    searchPostsCountResult: Int = 0
  ): PostService[RunF] = {
    val postRepo = PostRepositoryMock.create[Id](
      findAllResult,
      findCountResult,
      findByIdResult,
      findByTagSlugResult = findByTagSlugResult,
      findCountByTagSlugResult = findCountByTagSlugResult,
      incrementViewsResult = incrementViewsResult,
      searchPostsResult = searchPostsResult,
      searchPostsCountResult = searchPostsCountResult
    )
    val tagRepo = TagRepositoryMock.create[Id](
      findByPostIdResult = findByPostIdResult,
      findByPostIdsResult = findByPostIdsResult
    )
    PostServiceImpl.create[RunF, Id](postRepo, tagRepo, xa)
  }
}
