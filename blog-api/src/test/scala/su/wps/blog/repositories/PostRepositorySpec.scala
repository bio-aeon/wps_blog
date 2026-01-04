package su.wps.blog.repositories

import cats.syntax.traverse.*
import doobie.ConnectionIO
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless.*
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.PostId
import su.wps.blog.tools.DbTest
import su.wps.blog.tools.scalacheck.*
import su.wps.blog.tools.syntax.*
import su.wps.blog.tools.types.PosInt

class PostRepositorySpec extends Specification with DbTest {
  sequential

  lazy val repo: PostRepository[ConnectionIO] = PostRepositoryImpl.create[ConnectionIO]

  implicit val genUser: Gen[models.User] = arbitrary[models.User]
  implicit val genPost: Gen[models.Post] = arbitrary[models.Post]

  "PostRepository should" >> {
    "return only visible posts with limit and offset" >> {
      val test = for {
        r0 <- repo.findAllWithLimitAndOffset(10, 0)
        user = random[models.User]
        _ <- models.User.sql.insert(user)
        // Insert 10 visible and 5 hidden posts
        _ <- random[models.Post](10).zipWithIndex
          .map { case (p, i) => p.copy(id = PosInt(i), authorId = user.id, isHidden = false) }
          .traverse(models.Post.sql.insert)
        _ <- random[models.Post](5).zipWithIndex
          .map { case (p, i) => p.copy(id = PosInt(i + 10), authorId = user.id, isHidden = true) }
          .traverse(models.Post.sql.insert)
        r1 <- repo.findAllWithLimitAndOffset(20, 0)
      } yield (r0, r1)

      val (r0, r1) = test.runWithIO()
      r0 must haveLength(0)
      r1 must haveLength(10) // Only visible posts
      r1.forall(!_.isHidden) must beTrue
    }

    "return correct count excluding hidden posts" >> {
      val test = for {
        r0 <- repo.findCount
        user = random[models.User]
        _ <- models.User.sql.insert(user)
        // Insert 8 visible and 4 hidden posts
        _ <- random[models.Post](8).zipWithIndex
          .map { case (p, i) => p.copy(id = PosInt(i), authorId = user.id, isHidden = false) }
          .traverse(models.Post.sql.insert)
        _ <- random[models.Post](4).zipWithIndex
          .map { case (p, i) => p.copy(id = PosInt(i + 8), authorId = user.id, isHidden = true) }
          .traverse(models.Post.sql.insert)
        r1 <- repo.findCount
      } yield (r0, r1)

      val (r0, r1) = test.runWithIO()
      r0 mustEqual 0
      r1 mustEqual 8 // Only visible posts counted
    }

    "respect pagination with visible posts only" >> {
      val user = random[models.User]
      val test = for {
        _ <- models.User.sql.insert(user)
        // Insert 10 visible posts
        _ <- random[models.Post](10).zipWithIndex
          .map { case (p, i) => p.copy(id = PosInt(i), authorId = user.id, isHidden = false) }
          .traverse(models.Post.sql.insert)
        // Insert 5 hidden posts
        _ <- random[models.Post](5).zipWithIndex
          .map { case (p, i) => p.copy(id = PosInt(i + 10), authorId = user.id, isHidden = true) }
          .traverse(models.Post.sql.insert)
        page1 <- repo.findAllWithLimitAndOffset(3, 0)
        page2 <- repo.findAllWithLimitAndOffset(3, 3)
        page3 <- repo.findAllWithLimitAndOffset(3, 9)
      } yield (page1, page2, page3)

      val (page1, page2, page3) = test.runWithIO()
      page1 must haveLength(3)
      page2 must haveLength(3)
      page3 must haveLength(1) // Only 1 visible post left (10 - 9 = 1)
    }

    "return all posts including hidden for admin method" >> {
      val user = random[models.User]
      val test = for {
        _ <- models.User.sql.insert(user)
        // Insert 5 visible and 3 hidden posts
        _ <- random[models.Post](5).zipWithIndex
          .map { case (p, i) => p.copy(id = PosInt(i), authorId = user.id, isHidden = false) }
          .traverse(models.Post.sql.insert)
        _ <- random[models.Post](3).zipWithIndex
          .map { case (p, i) => p.copy(id = PosInt(i + 5), authorId = user.id, isHidden = true) }
          .traverse(models.Post.sql.insert)
        publicPosts <- repo.findAllWithLimitAndOffset(20, 0)
        allPosts <- repo.findAllWithLimitAndOffsetIncludeHidden(20, 0)
        publicCount <- repo.findCount
        totalCount <- repo.findCountIncludeHidden
      } yield (publicPosts, allPosts, publicCount, totalCount)

      val (publicPosts, allPosts, publicCount, totalCount) = test.runWithIO()
      publicPosts must haveLength(5)
      allPosts must haveLength(8) // All posts including hidden
      publicCount mustEqual 5
      totalCount mustEqual 8
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
  }
}
