package su.wps.blog.services

import cats.syntax.apply.*
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.Monad
import mouse.anyf.*
import su.wps.blog.models.api.*
import su.wps.blog.models.domain.AppErr.PostNotFound
import su.wps.blog.models.domain.*
import su.wps.blog.repositories.*
import tofu.Raise
import tofu.doobie.transactor.Txr

final class PostServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  postRepo: PostRepository[DB],
  tagRepo: TagRepository[DB],
  postTranslationRepo: PostTranslationRepository[DB],
  tagTranslationRepo: TagTranslationRepository[DB],
  xa: Txr[F, DB]
)(implicit R: Raise[F, AppErr])
    extends PostService[F] {
  import ServiceHelpers._

  private def fetchAndEnrichPosts(
    fetchPosts: DB[List[Post]],
    fetchCount: DB[Int],
    lang: String
  ): F[ListItemsResult[ListPostResult]] =
    (fetchPosts, fetchCount)
      .mapN((_, _))
      .flatMap { case (posts, count) =>
        val postIds = posts.flatMap(_.id)
        (
          postTranslationRepo.findAvailableLanguagesByPostIds(postIds),
          tagRepo.findByPostIds(postIds)
        ).mapN { case (langsByPost, tagsByPost) =>
          val tagsByPostId =
            tagsByPost.groupBy(_._1).map { case (pid, tags) => pid -> tags.map(_._2) }
          val items = posts.map { post =>
            val pid = post.nonEmptyId
            val tags = tagsByPostId.getOrElse(pid, Nil)
            ListPostResult(
              id = pid,
              name = post.name,
              shortText = nonEmpty(post.shortText),
              createdAt = post.createdAt,
              language = lang,
              tags = tagsToTagResults(tags),
              availableLanguages = langsByPost.getOrElse(pid, List(lang))
            )
          }
          ListItemsResult(items, count)
        }
      }
      .thrushK(xa.trans)

  def allPosts(lang: String, limit: Int, offset: Int): F[ListItemsResult[ListPostResult]] =
    fetchAndEnrichPosts(postRepo.findAllWithLimitAndOffset(limit, offset), postRepo.findCount, lang)

  def postsByTag(
    lang: String,
    tagSlug: String,
    limit: Int,
    offset: Int
  ): F[ListItemsResult[ListPostResult]] =
    fetchAndEnrichPosts(
      postRepo.findByTagSlug(tagSlug, limit, offset),
      postRepo.findCountByTagSlug(tagSlug),
      lang
    )

  def postById(lang: String, id: PostId): F[PostResult] =
    (
      postRepo.findById(id),
      tagRepo.findByPostId(id),
      postTranslationRepo.findAvailableLanguages(id)
    )
      .mapN((_, _, _))
      .thrushK(xa.trans)
      .flatMap { case (postOpt, tags, availLangs) =>
        postOpt match {
          case Some(post) =>
            PostResult(
              id = post.nonEmptyId,
              name = post.name,
              text = post.text,
              createdAt = post.createdAt,
              language = lang,
              tags = tagsToTagResults(tags),
              seo = buildSeo(post.metaTitle, post.metaDescription, post.metaKeywords),
              availableLanguages = if (availLangs.isEmpty) List(lang) else availLangs
            ).pure[F]
          case None =>
            R.raise(PostNotFound(id))
        }
      }

  def incrementViewCount(id: PostId): F[Unit] =
    postRepo.incrementViews(id).thrushK(xa.trans).void

  def searchPosts(
    lang: String,
    query: String,
    limit: Int,
    offset: Int
  ): F[ListItemsResult[ListPostResult]] =
    fetchAndEnrichPosts(
      postRepo.searchPosts(query, limit, offset),
      postRepo.searchPostsCount(query),
      lang
    )

  def recentPosts(lang: String, count: Int): F[List[ListPostResult]] =
    postRepo
      .findRecent(count)
      .flatMap { posts =>
        val postIds = posts.flatMap(_.id)
        (
          tagRepo.findByPostIds(postIds),
          postTranslationRepo.findAvailableLanguagesByPostIds(postIds)
        ).mapN { case (tagsByPost, langsByPost) =>
          val tagsByPostId =
            tagsByPost.groupBy(_._1).map { case (pid, tags) => pid -> tags.map(_._2) }
          posts.map { post =>
            val pid = post.nonEmptyId
            val tags = tagsByPostId.getOrElse(pid, Nil)
            ListPostResult(
              id = pid,
              name = post.name,
              shortText = nonEmpty(post.shortText),
              createdAt = post.createdAt,
              language = lang,
              tags = tagsToTagResults(tags),
              availableLanguages = langsByPost.getOrElse(pid, List(lang))
            )
          }
        }
      }
      .thrushK(xa.trans)

  private def buildSeo(title: String, description: String, keywords: String): Option[SeoResult] = {
    val seo = SeoResult(nonEmpty(title), nonEmpty(description), nonEmpty(keywords))
    if (seo.title.isDefined || seo.description.isDefined || seo.keywords.isDefined) Some(seo)
    else None
  }
}

object PostServiceImpl {
  def create[F[_]: Monad: Raise[*[_], AppErr], DB[_]: Monad](
    postRepo: PostRepository[DB],
    tagRepo: TagRepository[DB],
    postTranslationRepo: PostTranslationRepository[DB],
    tagTranslationRepo: TagTranslationRepository[DB],
    xa: Txr[F, DB]
  ): PostServiceImpl[F, DB] =
    new PostServiceImpl(postRepo, tagRepo, postTranslationRepo, tagTranslationRepo, xa)
}
