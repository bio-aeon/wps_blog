package su.wps.blog.services

import cats.Id
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless._
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.AppErr.PostNotFound
import su.wps.blog.models.domain.{Post, PostId}
import su.wps.blog.services.mocks.{PostRepositoryMock, TxrMock}
import su.wps.blog.tools.scalacheck._
import tofu.doobie.transactor.Txr

class PostServiceSpec extends Specification {
  type RunF[A] = Either[Throwable, A]

  val xa: Txr[RunF, Id] = TxrMock.create[RunF]

  implicit val genPost: Gen[Post] = for {
    id <- arbitrary[PostId]
    post <- arbitrary[Post].map(_.copy(id = Some(id)))
  } yield post

  "PostService should" >> {
    "return posts with total count by limit and offset" >> {
      val service = mkService(random[Post](5), 10)

      service.allPosts(5, 10) must beRight.which(r => r.items.length == 5 && r.total == 10)
    }

    "return post if exists" >> {
      val post = random[Post]
      val service = mkService(findByIdResult = Some(post))

      service.postById(post.nonEmptyId) must beRight.which(_.name == post.name)
    }

    "return an error if post doesn't exist" >> {
      val service = mkService()

      service.postById(PostId(1)) must beLeft(PostNotFound(PostId(1)))
    }
  }

  private def mkService(
    findAllResult: List[Post] = Nil,
    findCountResult: Int = 0,
    findByIdResult: Option[Post] = None
  ): PostService[RunF] = {
    val repo = PostRepositoryMock.create[Id](findAllResult, findCountResult, findByIdResult)
    PostServiceImpl.create[RunF, Id](repo, xa)
  }
}
