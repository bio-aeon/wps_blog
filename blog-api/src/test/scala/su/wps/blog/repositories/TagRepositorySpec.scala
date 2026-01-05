package su.wps.blog.repositories

import cats.syntax.functor.*
import cats.syntax.traverse.*
import doobie.ConnectionIO
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.ScalacheckShapeless.*
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.{PostId, TagId}
import su.wps.blog.tools.DbTest
import su.wps.blog.tools.scalacheck.*
import su.wps.blog.tools.syntax.*
import su.wps.blog.tools.types.{PosInt, Varchar}

import java.time.ZonedDateTime

class TagRepositorySpec extends Specification with DbTest {
  sequential

  lazy val repo: TagRepository[ConnectionIO] = TagRepositoryImpl.create[ConnectionIO]

  implicit val genUser: Gen[models.User] = arbitrary[models.User]
  implicit val genPost: Gen[models.Post] = arbitrary[models.Post]
  implicit val genTag: Gen[models.Tag] = arbitrary[models.Tag]
  implicit val genPostTag: Gen[models.PostTag] = arbitrary[models.PostTag]

  "TagRepository should" >> {
    "findByPostId returns tags for a specific post" >> {
      val test = for {
        user <- insertUser()
        post <- insertPost(user.id)
        tag1 <- insertTag(PosInt(1), "scala", "scala")
        tag2 <- insertTag(PosInt(2), "functional", "functional")
        tag3 <- insertTag(PosInt(3), "http4s", "http4s")
        _ <- linkPostToTags(post.id, List(tag1.id, tag2.id, tag3.id))
        result <- repo.findByPostId(PostId(post.id.value))
      } yield result

      val result = test.runWithIO()
      result must haveLength(3)
      result.map(_.name) must containAllOf(List("scala", "functional", "http4s"))
    }

    "findByPostId returns empty list for post with no tags" >> {
      val test = for {
        user <- insertUser()
        post <- insertPost(user.id)
        result <- repo.findByPostId(PostId(post.id.value))
      } yield result

      val result = test.runWithIO()
      result must beEmpty
    }

    "findByPostIds returns tags with their post id" >> {
      val test = for {
        user <- insertUser()
        post1 <- insertPost(user.id, PosInt(1))
        post2 <- insertPost(user.id, PosInt(2))
        tag1 <- insertTag(PosInt(1), "scala", "scala")
        tag2 <- insertTag(PosInt(2), "rust", "rust")
        _ <- linkPostToTags(post1.id, List(tag1.id))
        _ <- linkPostToTags(post2.id, List(tag1.id, tag2.id))
        result <- repo.findByPostIds(List(PostId(post1.id.value), PostId(post2.id.value)))
      } yield (result, PostId(post1.id.value), PostId(post2.id.value))

      val (result, post1Id, post2Id) = test.runWithIO()
      result must haveLength(3) // 1 tag for post1 + 2 tags for post2
      result.count(_._1 == post1Id) mustEqual 1
      result.count(_._1 == post2Id) mustEqual 2
    }

    "findByPostIds handles empty list" >> {
      val test = repo.findByPostIds(Nil)

      val result = test.runWithIO()
      result must beEmpty
    }

    "findAll returns all tags ordered by name" >> {
      val test = for {
        _ <- insertTag(PosInt(1), "zebra", "zebra")
        _ <- insertTag(PosInt(2), "alpha", "alpha")
        _ <- insertTag(PosInt(3), "middle", "middle")
        result <- repo.findAll
      } yield result

      val result = test.runWithIO()
      result must haveLength(3)
      result.map(_.name) mustEqual List("alpha", "middle", "zebra")
    }

    "findAllWithPostCounts excludes hidden posts from counts" >> {
      val test = for {
        user <- insertUser()
        tag <- insertTag(PosInt(1), "scala", "scala")
        visiblePost <- insertPost(user.id, PosInt(1), isHidden = false)
        hiddenPost <- insertPost(user.id, PosInt(2), isHidden = true)
        _ <- linkPostToTags(visiblePost.id, List(tag.id))
        _ <- linkPostToTags(hiddenPost.id, List(tag.id))
        result <- repo.findAllWithPostCounts
      } yield result

      val result = test.runWithIO()
      result must haveLength(1)
      result.head._2 mustEqual 1 // Only visible post counted
    }

    "findById returns the tag if it exists" >> {
      val test = for {
        tag <- insertTag(PosInt(1), "scala", "scala")
        result <- repo.findById(TagId(tag.id.value))
      } yield result

      val result = test.runWithIO()
      result must beSome.which(_.name == "scala")
    }

    "findById returns None for non-existent tag" >> {
      val test = repo.findById(TagId(9999))

      val result = test.runWithIO()
      result must beNone
    }
  }

  private def insertUser(): ConnectionIO[models.User] = {
    val user = random[models.User]
    models.User.sql.insert(user).map(_ => user)
  }

  private def insertPost(
    authorId: PosInt,
    id: PosInt = random[PosInt],
    isHidden: Boolean = false
  ): ConnectionIO[models.Post] = {
    val post = random[models.Post].copy(id = id, authorId = authorId, isHidden = isHidden)
    models.Post.sql.insert(post).map(_ => post)
  }

  private def insertTag(id: PosInt, name: String, slug: String): ConnectionIO[models.Tag] = {
    val tag = models.Tag(id, Varchar(name), Varchar(slug))
    models.Tag.sql.insert(tag).map(_ => tag)
  }

  private def linkPostToTags(postId: PosInt, tagIds: List[PosInt]): ConnectionIO[Unit] =
    tagIds.traverse { tagId =>
      val postTag = models.PostTag(postId, tagId, ZonedDateTime.now())
      models.PostTag.sql.insert(postTag)
    }.void
}
