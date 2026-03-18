package su.wps.blog.services

import cats.Monad
import cats.syntax.apply.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import mouse.anyf.*
import su.wps.blog.models.api.*
import su.wps.blog.models.domain.PostId
import su.wps.blog.repositories.*
import tofu.doobie.transactor.Txr

final class FeedServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  postRepo: PostRepository[DB],
  tagRepo: TagRepository[DB],
  pageRepo: PageRepository[DB],
  postTranslationRepo: PostTranslationRepository[DB],
  xa: Txr[F, DB]
) extends FeedService[F] {
  import ServiceHelpers._

  def getFeed(lang: String): F[FeedResult] =
    (postRepo.findAllVisible, tagRepo.findAllWithPostCounts, pageRepo.findAll)
      .mapN((_, _, _))
      .flatMap { case (posts, tagsWithCounts, pages) =>
        val postIds = posts.flatMap(_.id)
        (
          tagRepo.findByPostIds(postIds),
          postTranslationRepo.findAvailableLanguagesByPostIds(postIds)
        ).mapN { case (tagsByPost, langsByPost) =>
          val tagsByPostId =
            tagsByPost.groupBy(_._1).map { case (pid, tags) => pid -> tags.map(_._2) }
          (posts, tagsWithCounts, pages, tagsByPostId, langsByPost)
        }
      }
      .thrushK(xa.trans)
      .map { case (posts, tagsWithCounts, pages, tagsByPostId, langsByPost) =>
        val feedPosts = posts.map { post =>
          val pid = post.nonEmptyId
          val tags = tagsByPostId.getOrElse(pid, Nil)
          FeedPostItem(
            id = pid,
            name = post.name,
            shortText = nonEmpty(post.shortText),
            metaDescription = nonEmpty(post.metaDescription),
            createdAt = post.createdAt,
            language = lang,
            tags = tagsToTagResults(tags),
            availableLanguages = langsByPost.getOrElse(pid, List(lang))
          )
        }

        val feedPages = pages.map(p => FeedPageItem(p.url, p.title, p.createdAt))
        val feedTags = tagsWithCounts
          .filter(_._2 > 0)
          .map(_._1)
          .map(t => FeedTagItem(t.name, t.slug))

        FeedResult(feedPosts, feedPages, feedTags)
      }
}

object FeedServiceImpl {
  def create[F[_]: Monad, DB[_]: Monad](
    postRepo: PostRepository[DB],
    tagRepo: TagRepository[DB],
    pageRepo: PageRepository[DB],
    postTranslationRepo: PostTranslationRepository[DB],
    xa: Txr[F, DB]
  ): FeedServiceImpl[F, DB] =
    new FeedServiceImpl(postRepo, tagRepo, pageRepo, postTranslationRepo, xa)
}
