package su.wps.blog.repositories

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.traverse._
import doobie.ConnectionIO
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless._
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.PostId
import su.wps.blog.tools.DbTest
import su.wps.blog.tools.scalacheck._
import su.wps.blog.tools.syntax._
import su.wps.blog.tools.types.PosInt

class PostRepositorySpec extends Specification with DbTest {
  sequential

  lazy val repo: PostRepository[ConnectionIO] =
    PostRepositoryImpl.create[IO, ConnectionIO].unsafeRunSync()

  implicit val genUser: Gen[models.User] = arbitrary[models.User]
  implicit val genPost: Gen[models.Post] = arbitrary[models.Post]

  "PostRepository should" >> {
    "return all posts with limit and offset" >> {
      val test = for {
        r0 <- repo.findAllWithLimitAndOffset(10, 5)
        user = random[models.User]
        _ <- models.User.sql.insert(user)
        _ <- random[models.Post](20).zipWithIndex
          .map { case (p, i) => p.copy(id = PosInt(i), authorId = user.id) }
          .traverse(models.Post.sql.insert)
        r1 <- repo.findAllWithLimitAndOffset(10, 5)
      } yield (r0, r1)

      val (r0, r1) = test.runWithIO()
      r0 must haveLength(0)
      r1 must haveLength(10)
    }

    "return the post by id if it exists" >> {
      val postId = 1
      val test = for {
        r0 <- repo.findById(PostId(postId))
        user = random[models.User]
        _ <- models.User.sql.insert(user)
        post = random[models.Post].copy(id = PosInt(postId), authorId = user.id)
        _ <- models.Post.sql.insert(post)
        r1 <- repo.findById(PostId(postId))
      } yield (r0, r1)

      val (r0, r1) = test.runWithIO()
      r0 must beNone
      r1 must beSome
    }

    "return posts count" >> {
      val test = for {
        r0 <- repo.findCount
        user = random[models.User]
        _ <- models.User.sql.insert(user)
        _ <- random[models.Post](20).zipWithIndex
          .map { case (p, i) => p.copy(id = PosInt(i), authorId = user.id) }
          .traverse(models.Post.sql.insert)
        r1 <- repo.findCount
      } yield (r0, r1)

      val (r0, r1) = test.runWithIO()
      r0 mustEqual 0
      r1 mustEqual 20
    }
  }
}
