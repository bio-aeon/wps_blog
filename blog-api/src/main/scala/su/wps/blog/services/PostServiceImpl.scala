package su.wps.blog.services

import cats.syntax.apply.*
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.Monad
import io.scalaland.chimney.dsl.*
import mouse.anyf.*
import su.wps.blog.models.api.{ListItemsResult, ListPostResult, PostResult, TagResult}
import su.wps.blog.models.domain.AppErr.PostNotFound
import su.wps.blog.models.domain.{AppErr, Post, PostId, Tag}
import su.wps.blog.repositories.{PostRepository, TagRepository}
import tofu.Raise
import tofu.doobie.transactor.Txr

final class PostServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  postRepo: PostRepository[DB],
  tagRepo: TagRepository[DB],
  xa: Txr[F, DB]
)(implicit R: Raise[F, AppErr])
    extends PostService[F] {
  import PostServiceImpl._

  private def enrichPostsWithTags(posts: List[Post]): DB[(List[Post], Map[PostId, List[Tag]])] = {
    val postIds = posts.flatMap(_.id)
    tagRepo.findByPostIds(postIds).map { tagsByPost =>
      val tagsByPostId = tagsByPost.groupBy(_._1).map { case (pid, tags) => pid -> tags.map(_._2) }
      (posts, tagsByPostId)
    }
  }

  private def toListPostResults(
    posts: List[Post],
    tagsByPostId: Map[PostId, List[Tag]]
  ): List[ListPostResult] =
    posts.map { post =>
      val tags = tagsByPostId.getOrElse(post.nonEmptyId, Nil)
      post
        .into[ListPostResult]
        .withFieldComputed(_.id, _.nonEmptyId)
        .withFieldConst(_.tags, tagsToTagResults(tags))
        .transform
    }

  private def fetchPostsWithCount(
    fetchPosts: DB[List[Post]],
    fetchCount: DB[Int]
  ): F[ListItemsResult[ListPostResult]] =
    (fetchPosts, fetchCount)
      .mapN((_, _))
      .flatMap { case (posts, count) =>
        enrichPostsWithTags(posts).map((_, count))
      }
      .thrushK(xa.trans)
      .map { case ((posts, tagsByPostId), total) =>
        ListItemsResult(toListPostResults(posts, tagsByPostId), total)
      }

  def allPosts(limit: Int, offset: Int): F[ListItemsResult[ListPostResult]] =
    fetchPostsWithCount(postRepo.findAllWithLimitAndOffset(limit, offset), postRepo.findCount)

  def postsByTag(tagSlug: String, limit: Int, offset: Int): F[ListItemsResult[ListPostResult]] =
    fetchPostsWithCount(
      postRepo.findByTagSlug(tagSlug, limit, offset),
      postRepo.findCountByTagSlug(tagSlug)
    )

  def postById(id: PostId): F[PostResult] =
    (postRepo.findById(id), tagRepo.findByPostId(id))
      .mapN((_, _))
      .thrushK(xa.trans)
      .flatMap { case (postOpt, tags) =>
        postOpt match {
          case Some(post) =>
            post
              .into[PostResult]
              .withFieldConst(_.tags, tagsToTagResults(tags))
              .transform
              .pure[F]
          case None =>
            R.raise(PostNotFound(id))
        }
      }

  def incrementViewCount(id: PostId): F[Unit] =
    postRepo.incrementViews(id).thrushK(xa.trans).void

  def searchPosts(query: String, limit: Int, offset: Int): F[ListItemsResult[ListPostResult]] =
    fetchPostsWithCount(
      postRepo.searchPosts(query, limit, offset),
      postRepo.searchPostsCount(query)
    )

  def recentPosts(count: Int): F[List[ListPostResult]] =
    postRepo
      .findRecent(count)
      .flatMap(enrichPostsWithTags)
      .thrushK(xa.trans)
      .map { case (posts, tagsByPostId) =>
        toListPostResults(posts, tagsByPostId)
      }
}

object PostServiceImpl {
  private def tagsToTagResults(tags: List[Tag]): List[TagResult] =
    tags.map(t => t.into[TagResult].withFieldComputed(_.id, _.nonEmptyId).transform)

  def create[F[_]: Monad: Raise[*[_], AppErr], DB[_]: Monad](
    postRepo: PostRepository[DB],
    tagRepo: TagRepository[DB],
    xa: Txr[F, DB]
  ): PostServiceImpl[F, DB] =
    new PostServiceImpl(postRepo, tagRepo, xa)
}
