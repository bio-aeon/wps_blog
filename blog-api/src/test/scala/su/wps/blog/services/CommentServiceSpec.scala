package su.wps.blog.services

import cats.Id
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless.*
import org.specs2.mutable.Specification
import su.wps.blog.models.api.CreateCommentRequest
import su.wps.blog.models.domain.{Comment, CommentId, PostId}
import su.wps.blog.services.mocks.{CommentRepositoryMock, TxrMock}
import su.wps.blog.tools.scalacheck.*
import tofu.doobie.transactor.Txr

import java.time.ZonedDateTime

class CommentServiceSpec extends Specification {
  type RunF[A] = Either[Throwable, A]

  val xa: Txr[RunF, Id] = TxrMock.create[RunF]

  implicit val genComment: Gen[Comment] = for {
    id <- arbitrary[CommentId]
    comment <- arbitrary[Comment].map(_.copy(id = Some(id), parentId = None))
  } yield comment

  "CommentService should" >> {
    "return comments as threaded tree structure" >> {
      val rootComment = mkComment(CommentId(1), None)
      val reply1 = mkComment(CommentId(2), Some(1))
      val reply2 = mkComment(CommentId(3), Some(1))
      val comments = List(rootComment, reply1, reply2)
      val service = mkService(comments)

      val result = service.getCommentsForPost(PostId(1))

      result must beRight.which { r =>
        r.comments.length == 1 && r.comments.head.replies.length == 2
      }
    }

    "correctly nest deeply nested replies" >> {
      val root = mkComment(CommentId(1), None)
      val reply1 = mkComment(CommentId(2), Some(1))
      val subReply = mkComment(CommentId(3), Some(2))
      val subSubReply = mkComment(CommentId(4), Some(3))
      val comments = List(root, reply1, subReply, subSubReply)
      val service = mkService(comments)

      val result = service.getCommentsForPost(PostId(1))

      result must beRight.which { r =>
        r.comments.head.replies.head.replies.head.replies.nonEmpty
      }
    }

    "return total count of all comments (flat)" >> {
      val root = mkComment(CommentId(1), None)
      val reply1 = mkComment(CommentId(2), Some(1))
      val reply2 = mkComment(CommentId(3), Some(1))
      val root2 = mkComment(CommentId(4), None)
      val nestedReply = mkComment(CommentId(5), Some(2))
      val comments = List(root, reply1, reply2, root2, nestedReply)
      val service = mkService(comments)

      val result = service.getCommentsForPost(PostId(1))

      result must beRight.which(_.total == 5)
    }

    "sort root comments by createdAt" >> {
      val older = mkComment(CommentId(1), None, ZonedDateTime.now().minusHours(2))
      val newer = mkComment(CommentId(2), None, ZonedDateTime.now())
      val comments = List(newer, older)
      val service = mkService(comments)

      val result = service.getCommentsForPost(PostId(1))

      result must beRight.which { r =>
        r.comments.head.id.value == 1 && r.comments.last.id.value == 2
      }
    }

    "sort replies within each parent by createdAt" >> {
      val root = mkComment(CommentId(1), None)
      val olderReply = mkComment(CommentId(2), Some(1), ZonedDateTime.now().minusHours(2))
      val newerReply = mkComment(CommentId(3), Some(1), ZonedDateTime.now())
      val comments = List(root, newerReply, olderReply)
      val service = mkService(comments)

      val result = service.getCommentsForPost(PostId(1))

      result must beRight.which { r =>
        val replies = r.comments.head.replies
        replies.head.id.value == 2 && replies.last.id.value == 3
      }
    }

    "return empty result for post with no comments" >> {
      val service = mkService(Nil)

      val result = service.getCommentsForPost(PostId(1))

      result must beRight.which { r =>
        r.comments.isEmpty && r.total == 0
      }
    }

    "create a comment and return CommentResult" >> {
      val request = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
      val service = mkServiceForCreate()

      val result = service.createComment(PostId(1), request)

      result must beRight.which { r =>
        r.name == "Author" && r.text == "Comment text" && r.rating == 0
      }
    }

    "create a reply linked to parent comment" >> {
      val request = CreateCommentRequest("Replier", "reply@example.com", "Reply text", Some(1))
      val service = mkServiceForCreate()

      val result = service.createComment(PostId(1), request)

      result must beRight.which(_.id.value > 0)
    }

    "return comment with generated id after creation" >> {
      val request = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
      val service = mkServiceForCreate(generatedId = CommentId(42))

      val result = service.createComment(PostId(1), request)

      result must beRight.which(_.id.value == 42)
    }

    "return comment with zero rating for newly created comment" >> {
      val request = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
      val service = mkServiceForCreate()

      val result = service.createComment(PostId(1), request)

      result must beRight.which(_.rating == 0)
    }

    "return comment with empty replies list for newly created comment" >> {
      val request = CreateCommentRequest("Author", "test@example.com", "Comment text", None)
      val service = mkServiceForCreate()

      val result = service.createComment(PostId(1), request)

      result must beRight.which(_.replies.isEmpty)
    }
  }

  private def mkComment(
    id: CommentId,
    parentId: Option[Int],
    createdAt: ZonedDateTime = ZonedDateTime.now()
  ): Comment =
    Comment(
      text = s"Comment ${id.value}",
      name = s"Author ${id.value}",
      email = s"author${id.value}@example.com",
      postId = 1,
      rating = 0,
      createdAt = createdAt,
      parentId = parentId,
      id = Some(id)
    )

  private def mkService(comments: List[Comment]): CommentService[RunF] = {
    val commentRepo = CommentRepositoryMock.create[Id](comments)
    CommentServiceImpl.create[RunF, Id](commentRepo, xa)
  }

  private def mkServiceForCreate(generatedId: CommentId = CommentId(1)): CommentService[RunF] = {
    val commentRepo = CommentRepositoryMock.create[Id](
      insertResult = comment => comment.copy(id = Some(generatedId))
    )
    CommentServiceImpl.create[RunF, Id](commentRepo, xa)
  }
}
