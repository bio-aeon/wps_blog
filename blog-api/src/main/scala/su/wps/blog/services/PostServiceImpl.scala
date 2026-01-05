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
import su.wps.blog.models.domain.{AppErr, PostId, Tag}
import su.wps.blog.repositories.{PostRepository, TagRepository}
import tofu.Raise
import tofu.doobie.transactor.Txr

final class PostServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  postRepo: PostRepository[DB],
  tagRepo: TagRepository[DB],
  xa: Txr[F, DB]
)(implicit R: Raise[F, AppErr])
    extends PostService[F] {

  private def tagsToTagResults(tags: List[Tag]): List[TagResult] =
    tags.map(t => t.into[TagResult].withFieldComputed(_.id, _.nonEmptyId).transform)

  def allPosts(limit: Int, offset: Int): F[ListItemsResult[ListPostResult]] =
    (postRepo.findAllWithLimitAndOffset(limit, offset), postRepo.findCount)
      .mapN((_, _))
      .flatMap { case (posts, count) =>
        val postIds = posts.flatMap(_.id)
        tagRepo.findByPostIds(postIds).map((posts, count, _))
      }
      .thrushK(xa.trans)
      .map { case (posts, total, tagsByPost) =>
        val tagsByPostId =
          tagsByPost.groupBy(_._1).map { case (pid, tags) => pid -> tags.map(_._2) }
        ListItemsResult(
          posts.map { post =>
            val tags = tagsByPostId.get(post.nonEmptyId).toList.flatten
            post
              .into[ListPostResult]
              .withFieldComputed(_.id, _.nonEmptyId)
              .withFieldConst(_.tags, tagsToTagResults(tags))
              .transform
          },
          total
        )
      }

  def postsByTag(tagSlug: String, limit: Int, offset: Int): F[ListItemsResult[ListPostResult]] =
    (postRepo.findByTagSlug(tagSlug, limit, offset), postRepo.findCountByTagSlug(tagSlug))
      .mapN((_, _))
      .flatMap { case (posts, count) =>
        val postIds = posts.flatMap(_.id)
        tagRepo.findByPostIds(postIds).map((posts, count, _))
      }
      .thrushK(xa.trans)
      .map { case (posts, total, tagsByPost) =>
        val tagsByPostId =
          tagsByPost.groupBy(_._1).map { case (pid, tags) => pid -> tags.map(_._2) }
        ListItemsResult(
          posts.map { post =>
            val tags = tagsByPostId.get(post.nonEmptyId).toList.flatten
            post
              .into[ListPostResult]
              .withFieldComputed(_.id, _.nonEmptyId)
              .withFieldConst(_.tags, tagsToTagResults(tags))
              .transform
          },
          total
        )
      }

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
}

object PostServiceImpl {
  def create[F[_]: Monad: Raise[*[_], AppErr], DB[_]: Monad](
    postRepo: PostRepository[DB],
    tagRepo: TagRepository[DB],
    xa: Txr[F, DB]
  ): PostServiceImpl[F, DB] =
    new PostServiceImpl(postRepo, tagRepo, xa)
}
