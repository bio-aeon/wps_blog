package su.wps.blog.services

import cats.Id
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless.*
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.*
import su.wps.blog.services.mocks.*
import su.wps.blog.tools.scalacheck.*
import tofu.doobie.transactor.Txr

class FeedServiceSpec extends Specification {
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

  implicit val genPage: Gen[Page] = for {
    id <- arbitrary[PageId]
    page <- arbitrary[Page].map(_.copy(id = Some(id)))
  } yield page

  "FeedService" >> {
    "getFeed" >> {
      "returns all visible posts" >> {
        val posts = random[Post](3)
        val service = mkService(findAllVisibleResult = posts)

        service.getFeed("en") must beRight.which(_.posts.length == 3)
      }

      "returns all pages" >> {
        val pages = random[Page](2)
        val service = mkService(findAllResult = pages)

        service.getFeed("en") must beRight.which(_.pages.length == 2)
      }

      "returns tags with post counts excluding empty tags" >> {
        val tag1 = Tag("scala", "scala", Some(TagId(1)))
        val tag2 = Tag("rust", "rust", Some(TagId(2)))
        val tag3 = Tag("empty", "empty", Some(TagId(3)))
        val tagsWithCounts = List((tag1, 5), (tag2, 3), (tag3, 0))
        val service = mkService(findAllWithPostCountsResult = tagsWithCounts)

        service.getFeed("en") must beRight.which(_.tags.length == 2)
      }

      "maps non-empty metaDescription to Some" >> {
        val post = random[Post].copy(metaDescription = "Description")
        val service = mkService(findAllVisibleResult = List(post))

        service.getFeed("en") must beRight.which { r =>
          r.posts.head.metaDescription.contains("Description")
        }
      }

      "maps empty metaDescription to None" >> {
        val post = random[Post].copy(metaDescription = "")
        val service = mkService(findAllVisibleResult = List(post))

        service.getFeed("en") must beRight.which(_.posts.head.metaDescription.isEmpty)
      }

      "includes tags per post" >> {
        val posts =
          List(random[Post].copy(id = Some(PostId(1))), random[Post].copy(id = Some(PostId(2))))
        val tag = Tag("scala", "scala", Some(TagId(1)))
        val tagsByPost = List((PostId(1), tag))
        val service = mkService(findAllVisibleResult = posts, findByPostIdsResult = tagsByPost)

        service.getFeed("en") must beRight.which { r =>
          r.posts.head.tags.length == 1 && r.posts(1).tags.isEmpty
        }
      }

      "returns empty feed when no data exists" >> {
        val service = mkService()

        service.getFeed("en") must beRight.which { r =>
          r.posts.isEmpty && r.pages.isEmpty && r.tags.isEmpty
        }
      }
    }
  }

  private def mkService(
    findAllVisibleResult: List[Post] = Nil,
    findAllWithPostCountsResult: List[(Tag, Int)] = Nil,
    findAllResult: List[Page] = Nil,
    findByPostIdsResult: List[(PostId, Tag)] = Nil
  ): FeedService[RunF] = {
    val postRepo =
      PostRepositoryMock.create[Id](findAllVisibleResult = findAllVisibleResult)
    val tagRepo = TagRepositoryMock.create[Id](
      findAllWithPostCountsResult = findAllWithPostCountsResult,
      findByPostIdsResult = findByPostIdsResult
    )
    val pageRepo = PageRepositoryMock.create[Id](findAllResult = findAllResult)
    val postTranslationRepo = PostTranslationRepositoryMock.create[Id]()
    FeedServiceImpl.create[RunF, Id](postRepo, tagRepo, pageRepo, postTranslationRepo, xa)
  }
}
