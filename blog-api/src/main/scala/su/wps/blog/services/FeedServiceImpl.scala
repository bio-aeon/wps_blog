package su.wps.blog.services

import cats.Monad
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.scalaland.chimney.dsl.*
import mouse.anyf.*
import su.wps.blog.models.api.*
import su.wps.blog.models.domain.{PostId, Tag}
import su.wps.blog.repositories.{PageRepository, PostRepository, TagRepository}
import tofu.doobie.transactor.Txr

final class FeedServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  postRepo: PostRepository[DB],
  tagRepo: TagRepository[DB],
  pageRepo: PageRepository[DB],
  xa: Txr[F, DB]
) extends FeedService[F] {
  import ServiceHelpers._

  def getFeed: F[FeedResult] =
    (postRepo.findAllVisible, tagRepo.findAllWithPostCounts, pageRepo.findAll)
      .mapN((_, _, _))
      .flatMap { case (posts, tagsWithCounts, pages) =>
        val postIds = posts.flatMap(_.id)
        tagRepo.findByPostIds(postIds).map { tagsByPost =>
          val tagsByPostId =
            tagsByPost.groupBy(_._1).map { case (pid, tags) => pid -> tags.map(_._2) }
          (posts, tagsWithCounts, pages, tagsByPostId)
        }
      }
      .thrushK(xa.trans)
      .map { case (posts, tagsWithCounts, pages, tagsByPostId) =>
        val feedPosts = posts.map { post =>
          val tags = tagsByPostId.getOrElse(post.nonEmptyId, Nil)
          post
            .into[FeedPostItem]
            .withFieldComputed(_.id, _.nonEmptyId)
            .withFieldComputed(_.metaDescription, p => nonEmpty(p.metaDescription))
            .withFieldConst(_.tags, tagsToTagResults(tags))
            .transform
        }

        val feedPages = pages.map(_.into[FeedPageItem].transform)

        val feedTags = tagsWithCounts
          .filter(_._2 > 0)
          .map(_._1.into[FeedTagItem].transform)

        FeedResult(feedPosts, feedPages, feedTags)
      }
}

object FeedServiceImpl {
  def create[F[_]: Monad, DB[_]: Monad](
    postRepo: PostRepository[DB],
    tagRepo: TagRepository[DB],
    pageRepo: PageRepository[DB],
    xa: Txr[F, DB]
  ): FeedServiceImpl[F, DB] =
    new FeedServiceImpl(postRepo, tagRepo, pageRepo, xa)
}
