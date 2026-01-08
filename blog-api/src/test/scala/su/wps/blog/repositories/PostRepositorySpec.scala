package su.wps.blog.repositories

import cats.syntax.apply.*
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
import su.wps.blog.tools.types.{PosInt, Varchar, W}

class PostRepositorySpec extends Specification with DbTest {
  sequential

  lazy val repo: PostRepository[ConnectionIO] = PostRepositoryImpl.create[ConnectionIO]

  implicit val genUser: Gen[models.User] = arbitrary[models.User]
  implicit val genPost: Gen[models.Post] = arbitrary[models.Post]
  implicit val genTag: Gen[models.Tag] = arbitrary[models.Tag]

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

    "return posts matching the tag slug" >> {
      val user = random[models.User]
      val tag = random[models.Tag].copy(id = PosInt(1), slug = Varchar("scala"))
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Tag.sql.insert(tag)
        // Create 2 posts tagged with "scala"
        taggedPost1 = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
        taggedPost2 = random[models.Post].copy(id = PosInt(2), authorId = user.id, isHidden = false)
        _ <- models.Post.sql.insert(taggedPost1)
        _ <- models.Post.sql.insert(taggedPost2)
        _ <- models.PostTag.sql.insert(models.PostTag(PosInt(1), PosInt(1), taggedPost1.createdAt))
        _ <- models.PostTag.sql.insert(models.PostTag(PosInt(2), PosInt(1), taggedPost2.createdAt))
        // Create untagged post
        untaggedPost = random[models.Post].copy(
          id = PosInt(3),
          authorId = user.id,
          isHidden = false
        )
        _ <- models.Post.sql.insert(untaggedPost)
        result <- repo.findByTagSlug("scala", 10, 0)
      } yield result

      val result = test.runWithIO()
      result must haveLength(2)
      result.map(_.nonEmptyId.value) must contain(exactly(1, 2))
    }

    "exclude hidden posts even if tagged" >> {
      val user = random[models.User]
      val tag = random[models.Tag].copy(id = PosInt(1), slug = Varchar("rust"))
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Tag.sql.insert(tag)
        visiblePost = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
        hiddenPost = random[models.Post].copy(id = PosInt(2), authorId = user.id, isHidden = true)
        _ <- models.Post.sql.insert(visiblePost)
        _ <- models.Post.sql.insert(hiddenPost)
        _ <- models.PostTag.sql.insert(models.PostTag(PosInt(1), PosInt(1), visiblePost.createdAt))
        _ <- models.PostTag.sql.insert(models.PostTag(PosInt(2), PosInt(1), hiddenPost.createdAt))
        result <- repo.findByTagSlug("rust", 10, 0)
      } yield result

      val result = test.runWithIO()
      result must haveLength(1)
      result.head.nonEmptyId.value mustEqual 1
    }

    "return empty list for non-existent tag" >> {
      val user = random[models.User]
      val test = for {
        _ <- models.User.sql.insert(user)
        post = random[models.Post].copy(id = PosInt(1), authorId = user.id, isHidden = false)
        _ <- models.Post.sql.insert(post)
        result <- repo.findByTagSlug("nonexistent", 10, 0)
      } yield result

      val result = test.runWithIO()
      result must beEmpty
    }

    "respect pagination for posts by tag" >> {
      val user = random[models.User]
      val tag = random[models.Tag].copy(id = PosInt(1), slug = Varchar("fp"))
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Tag.sql.insert(tag)
        // Create 5 posts tagged with "fp"
        _ <- (1 to 5).toList.traverse { i =>
          val post = random[models.Post].copy(id = PosInt(i), authorId = user.id, isHidden = false)
          models.Post.sql.insert(post) *>
            models.PostTag.sql.insert(models.PostTag(PosInt(i), PosInt(1), post.createdAt))
        }
        page1 <- repo.findByTagSlug("fp", 2, 0)
        page2 <- repo.findByTagSlug("fp", 2, 2)
        page3 <- repo.findByTagSlug("fp", 2, 4)
      } yield (page1, page2, page3)

      val (page1, page2, page3) = test.runWithIO()
      page1 must haveLength(2)
      page2 must haveLength(2)
      page3 must haveLength(1)
    }

    "return correct count for posts by tag slug" >> {
      val user = random[models.User]
      val tag = random[models.Tag].copy(id = PosInt(1), slug = Varchar("cats"))
      val test = for {
        _ <- models.User.sql.insert(user)
        _ <- models.Tag.sql.insert(tag)
        // Create 3 visible and 2 hidden posts tagged with "cats"
        _ <- (1 to 3).toList.traverse { i =>
          val post = random[models.Post].copy(id = PosInt(i), authorId = user.id, isHidden = false)
          models.Post.sql.insert(post) *>
            models.PostTag.sql.insert(models.PostTag(PosInt(i), PosInt(1), post.createdAt))
        }
        _ <- (4 to 5).toList.traverse { i =>
          val post = random[models.Post].copy(id = PosInt(i), authorId = user.id, isHidden = true)
          models.Post.sql.insert(post) *>
            models.PostTag.sql.insert(models.PostTag(PosInt(i), PosInt(1), post.createdAt))
        }
        count <- repo.findCountByTagSlug("cats")
      } yield count

      val count = test.runWithIO()
      count mustEqual 3
    }

    "increment view count by 1 for visible post" >> {
      val postId = 1
      val initialViews = 10
      val user = random[models.User]
      val test = for {
        _ <- models.User.sql.insert(user)
        post = random[models.Post].copy(
          id = PosInt(postId),
          authorId = user.id,
          isHidden = false,
          views = PosInt(initialViews)
        )
        _ <- models.Post.sql.insert(post)
        rowsUpdated <- repo.incrementViews(PostId(postId))
        updated <- repo.findById(PostId(postId))
      } yield (rowsUpdated, updated)

      val (rowsUpdated, updated) = test.runWithIO()
      rowsUpdated mustEqual 1
      updated must beSome.which(p => p.views == initialViews + 1)
    }

    "not increment views for hidden posts" >> {
      val postId = 1
      val initialViews = 10
      val user = random[models.User]
      val test = for {
        _ <- models.User.sql.insert(user)
        post = random[models.Post].copy(
          id = PosInt(postId),
          authorId = user.id,
          isHidden = true,
          views = PosInt(initialViews)
        )
        _ <- models.Post.sql.insert(post)
        rowsUpdated <- repo.incrementViews(PostId(postId))
        unchanged <- repo.findById(PostId(postId))
      } yield (rowsUpdated, unchanged)

      val (rowsUpdated, unchanged) = test.runWithIO()
      rowsUpdated mustEqual 0
      unchanged must beSome.which(p => p.views == initialViews)
    }

    "return 0 for non-existent post" >> {
      val test = repo.incrementViews(PostId(99999))

      val rowsUpdated = test.runWithIO()
      rowsUpdated mustEqual 0
    }

    "search posts by query matching name" >> {
      val user = random[models.User]
      val test = for {
        _ <- models.User.sql.insert(user)
        // Create posts with specific content for search
        post1 = random[models.Post].copy(
          id = PosInt(1),
          authorId = user.id,
          isHidden = false,
          name = Varchar("Introduction to Scala Programming"),
          shortText = Varchar("Learn basics"),
          text = Varchar("Full article content")
        )
        post2 = random[models.Post].copy(
          id = PosInt(2),
          authorId = user.id,
          isHidden = false,
          name = Varchar("Python for Beginners"),
          shortText = Varchar("Python basics"),
          text = Varchar("Another article")
        )
        _ <- models.Post.sql.insert(post1)
        _ <- models.Post.sql.insert(post2)
        result <- repo.searchPosts("Scala", 10, 0)
      } yield result

      val result = test.runWithIO()
      result must haveLength(1)
      result.head.name must contain("Scala")
    }

    "search posts matching short_text content" >> {
      val user = random[models.User]
      val test = for {
        _ <- models.User.sql.insert(user)
        post1 = random[models.Post].copy(
          id = PosInt(1),
          authorId = user.id,
          isHidden = false,
          name = Varchar("Post One"),
          shortText = Varchar("This is about functional programming patterns"),
          text = Varchar("Some content")
        )
        post2 = random[models.Post].copy(
          id = PosInt(2),
          authorId = user.id,
          isHidden = false,
          name = Varchar("Post Two"),
          shortText = Varchar("Object oriented design"),
          text = Varchar("Other content")
        )
        _ <- models.Post.sql.insert(post1)
        _ <- models.Post.sql.insert(post2)
        result <- repo.searchPosts("functional", 10, 0)
      } yield result

      val result = test.runWithIO()
      result must haveLength(1)
      result.head.shortText must contain("functional")
    }

    "exclude hidden posts from search results" >> {
      val user = random[models.User]
      val test = for {
        _ <- models.User.sql.insert(user)
        visiblePost = random[models.Post].copy(
          id = PosInt(1),
          authorId = user.id,
          isHidden = false,
          name = Varchar("Public Haskell Tutorial"),
          shortText = Varchar("Haskell basics"),
          text = Varchar("Content")
        )
        hiddenPost = random[models.Post].copy(
          id = PosInt(2),
          authorId = user.id,
          isHidden = true,
          name = Varchar("Hidden Haskell Advanced"),
          shortText = Varchar("Advanced Haskell"),
          text = Varchar("Content")
        )
        _ <- models.Post.sql.insert(visiblePost)
        _ <- models.Post.sql.insert(hiddenPost)
        result <- repo.searchPosts("Haskell", 10, 0)
      } yield result

      val result = test.runWithIO()
      result must haveLength(1)
      result.head.isHidden must beFalse
    }

    "return empty list when no posts match search query" >> {
      val user = random[models.User]
      val test = for {
        _ <- models.User.sql.insert(user)
        post = random[models.Post].copy(
          id = PosInt(1),
          authorId = user.id,
          isHidden = false,
          name = Varchar("Java Tutorial"),
          shortText = Varchar("Java basics"),
          text = Varchar("Content")
        )
        _ <- models.Post.sql.insert(post)
        result <- repo.searchPosts("Nonexistent Language", 10, 0)
      } yield result

      val result = test.runWithIO()
      result must beEmpty
    }

    "return correct count for search results" >> {
      val user = random[models.User]
      val test = for {
        _ <- models.User.sql.insert(user)
        // Create 3 posts matching "database"
        _ <- (1 to 3).toList.traverse { i =>
          val post = random[models.Post].copy(
            id = PosInt(i),
            authorId = user.id,
            isHidden = false,
            name = Varchar(s"Database Tutorial $i"),
            shortText = Varchar("Learn database"),
            text = Varchar("Content")
          )
          models.Post.sql.insert(post)
        }
        // Create 2 posts not matching
        _ <- (4 to 5).toList.traverse { i =>
          val post = random[models.Post].copy(
            id = PosInt(i),
            authorId = user.id,
            isHidden = false,
            name = Varchar(s"Other Topic $i"),
            shortText = Varchar("Something else"),
            text = Varchar("Content")
          )
          models.Post.sql.insert(post)
        }
        count <- repo.searchPostsCount("database")
      } yield count

      val count = test.runWithIO()
      count mustEqual 3
    }

    "respect pagination in search results" >> {
      val user = random[models.User]
      val test = for {
        _ <- models.User.sql.insert(user)
        // Create 5 posts matching "programming"
        _ <- (1 to 5).toList.traverse { i =>
          val post = random[models.Post].copy(
            id = PosInt(i),
            authorId = user.id,
            isHidden = false,
            name = Varchar(s"Programming Guide $i"),
            shortText = Varchar("Learn programming"),
            text = Varchar("Content")
          )
          models.Post.sql.insert(post)
        }
        page1 <- repo.searchPosts("programming", 2, 0)
        page2 <- repo.searchPosts("programming", 2, 2)
        page3 <- repo.searchPosts("programming", 2, 4)
      } yield (page1, page2, page3)

      val (page1, page2, page3) = test.runWithIO()
      page1 must haveLength(2)
      page2 must haveLength(2)
      page3 must haveLength(1)
    }
  }
}
