package su.wps.blog.repositories

import cats.syntax.traverse.*
import doobie.ConnectionIO
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless.*
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.{CommentId, PostId}
import su.wps.blog.tools.DbTest
import su.wps.blog.tools.scalacheck.*
import su.wps.blog.tools.syntax.*
import su.wps.blog.tools.types.{PosInt, Varchar, W}

class CommentRepositorySpec extends Specification with DbTest {
  sequential

  lazy val repo: CommentRepository[ConnectionIO] = CommentRepositoryImpl.create[ConnectionIO]

  implicit val genUser: Gen[models.User] = arbitrary[models.User]
  implicit val genPost: Gen[models.Post] = arbitrary[models.Post]
  implicit val genComment: Gen[models.Comment] = arbitrary[models.Comment]
  implicit val genCommentRater: Gen[models.CommentRater] = arbitrary[models.CommentRater]

  "CommentRepository" >> {
    "returns all comments for a post" >> {
      val user = random[models.User]
      val post = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Post.sql.insert(post)
        // Create 3 comments for post 1
        _ <- (1 to 3).toList.traverse { i =>
          val comment =
            random[models.Comment].copy(id = PosInt(i), postId = PosInt(1), parentId = None)
          models.Comment.sql.insert(comment)
        }
        result <- repo.findCommentsByPostId(PostId(1))
      } yield result

      val result = test.runWithIO()
      result must haveLength(3)
    }

    "returns comments ordered by created_at ASC" >> {
      val user = random[models.User]
      val post = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Post.sql.insert(post)
        _ <- (1 to 5).toList.traverse { i =>
          val comment =
            random[models.Comment].copy(id = PosInt(i), postId = PosInt(1), parentId = None)
          models.Comment.sql.insert(comment)
        }
        result <- repo.findCommentsByPostId(PostId(1))
      } yield result

      val result = test.runWithIO()
      val dates = result.map(_.createdAt)
      dates mustEqual dates.sorted
    }

    "returns empty list for post with no comments" >> {
      val user = random[models.User]
      val post = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Post.sql.insert(post)
        result <- repo.findCommentsByPostId(PostId(1))
      } yield result

      val result = test.runWithIO()
      result must beEmpty
    }

    "includes nested replies (parent_id references)" >> {
      val user = random[models.User]
      val post = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Post.sql.insert(post)
        rootComment = random[models.Comment].copy(
          id = PosInt(1),
          postId = PosInt(1),
          parentId = None
        )
        reply = random[models.Comment].copy(
          id = PosInt(2),
          postId = PosInt(1),
          parentId = Some(PosInt(1))
        )
        _ <- models.Comment.sql.insert(rootComment)
        _ <- models.Comment.sql.insert(reply)
        result <- repo.findCommentsByPostId(PostId(1))
      } yield result

      val result = test.runWithIO()
      result must haveLength(2)
      val replyComment = result.find(_.id.exists(_.value == 2))
      replyComment must beSome.which(_.parentId.contains(1))
    }

    "checks if IP has rated a comment (not rated)" >> {
      val user = random[models.User]
      val post = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Post.sql.insert(post)
        comment = random[models.Comment].copy(id = PosInt(1), postId = PosInt(1), parentId = None)
        _ <- models.Comment.sql.insert(comment)
        result <- repo.hasRated(CommentId(1), "192.168.1.1")
      } yield result

      val result = test.runWithIO()
      result must beFalse
    }

    "checks if IP has rated a comment (already rated)" >> {
      val user = random[models.User]
      val post = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Post.sql.insert(post)
        comment = random[models.Comment].copy(id = PosInt(1), postId = PosInt(1), parentId = None)
        _ <- models.Comment.sql.insert(comment)
        rater = random[models.CommentRater].copy(
          id = PosInt(1),
          ip = Varchar("192.168.1.1"),
          commentId = PosInt(1)
        )
        _ <- models.CommentRater.sql.insert(rater)
        result <- repo.hasRated(CommentId(1), "192.168.1.1")
      } yield result

      val result = test.runWithIO()
      result must beTrue
    }

    "inserts rater successfully" >> {
      val user = random[models.User]
      val post = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Post.sql.insert(post)
        comment = random[models.Comment].copy(id = PosInt(1), postId = PosInt(1), parentId = None)
        _ <- models.Comment.sql.insert(comment)
        beforeInsert <- repo.hasRated(CommentId(1), "10.0.0.1")
        _ <- repo.insertRater(CommentId(1), "10.0.0.1")
        afterInsert <- repo.hasRated(CommentId(1), "10.0.0.1")
      } yield (beforeInsert, afterInsert)

      val (beforeInsert, afterInsert) = test.runWithIO()
      beforeInsert must beFalse
      afterInsert must beTrue
    }

    "updates rating by positive delta" >> {
      val user = random[models.User]
      val post = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
      val initialRating = 5
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Post.sql.insert(post)
        comment = random[models.Comment].copy(
          id = PosInt(1),
          postId = PosInt(1),
          parentId = None,
          rating = initialRating
        )
        _ <- models.Comment.sql.insert(comment)
        rowsUpdated <- repo.updateRating(CommentId(1), 1)
        comments <- repo.findCommentsByPostId(PostId(1))
      } yield (rowsUpdated, comments)

      val (rowsUpdated, comments) = test.runWithIO()
      rowsUpdated mustEqual 1
      comments.find(_.id.exists(_.value == 1)) must beSome.which(_.rating == initialRating + 1)
    }

    "updates rating by negative delta" >> {
      val user = random[models.User]
      val post = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
      val initialRating = 5
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Post.sql.insert(post)
        comment = random[models.Comment].copy(
          id = PosInt(1),
          postId = PosInt(1),
          parentId = None,
          rating = initialRating
        )
        _ <- models.Comment.sql.insert(comment)
        rowsUpdated <- repo.updateRating(CommentId(1), -1)
        comments <- repo.findCommentsByPostId(PostId(1))
      } yield (rowsUpdated, comments)

      val (rowsUpdated, comments) = test.runWithIO()
      rowsUpdated mustEqual 1
      comments.find(_.id.exists(_.value == 1)) must beSome.which(_.rating == initialRating - 1)
    }

    "inserts comment and returns with generated id" >> {
      val user = random[models.User]
      val post = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Post.sql.insert(post)
        comment = su.wps.blog.models.domain.Comment(
          text = "Test comment",
          name = "Author",
          email = "test@example.com",
          postId = 1,
          rating = 0,
          createdAt = java.time.ZonedDateTime.now(),
          parentId = None,
          isApproved = true,
          id = None
        )
        inserted <- repo.insert(comment)
      } yield inserted

      val result = test.runWithIO()
      result.id must beSome
      result.text mustEqual "Test comment"
      result.name mustEqual "Author"
    }

    "inserts reply comment with parent_id" >> {
      val user = random[models.User]
      val post = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Post.sql.insert(post)
        // Insert root comment via repo to get a valid ID
        rootComment = su.wps.blog.models.domain.Comment(
          text = "Root comment",
          name = "Author",
          email = "author@example.com",
          postId = 1,
          rating = 0,
          createdAt = java.time.ZonedDateTime.now(),
          parentId = None,
          isApproved = true,
          id = None
        )
        insertedRoot <- repo.insert(rootComment)
        rootId = insertedRoot.id.get.value
        reply = su.wps.blog.models.domain.Comment(
          text = "Reply text",
          name = "Replier",
          email = "reply@example.com",
          postId = 1,
          rating = 0,
          createdAt = java.time.ZonedDateTime.now(),
          parentId = Some(rootId),
          isApproved = true,
          id = None
        )
        insertedReply <- repo.insert(reply)
      } yield (rootId, insertedReply)

      val (rootId, result) = test.runWithIO()
      result.parentId must beSome(rootId)
    }
  }
}
