package su.wps.blog.services

import cats.data.OptionT
import cats.{Applicative, Monad}
import cats.syntax.functor._
import cats.syntax.semigroupal._
import mouse.anyf._
import su.wps.blog.models.api.{ListItemsResult, ListPostResult, PostResult}
import su.wps.blog.models.domain.{AppErr, PostId}
import su.wps.blog.repositories.PostRepository
import io.scalaland.chimney.dsl._
import su.wps.blog.models.domain.AppErr.PostNotFound
import tofu.Raise
import tofu.doobie.transactor.Txr

final class PostServiceImpl[F[_]: Monad, DB[_]: Applicative] private (
  postRepo: PostRepository[DB],
  xa: Txr[F, DB]
)(implicit R: Raise[F, AppErr])
    extends PostService[F] {
  def allPosts(limit: Int, offset: Int): F[ListItemsResult[ListPostResult]] =
    postRepo
      .findAllWithLimitAndOffset(limit, offset)
      .product(postRepo.findCount)
      .thrushK(xa.trans)
      .map {
        case (posts, total) =>
          ListItemsResult(
            posts.map(x => x.into[ListPostResult].withFieldComputed(_.id, _.nonEmptyId).transform),
            total
          )
      }

  def postById(id: PostId): F[PostResult] =
    OptionT(postRepo.findById(id).thrushK(xa.trans))
      .map(_.into[PostResult].transform)
      .getOrElseF(R.raise(PostNotFound(id)))
}

object PostServiceImpl {
  def create[F[_]: Monad: Raise[*[_], AppErr], DB[_]: Applicative](
    postRepo: PostRepository[DB],
    xa: Txr[F, DB]
  ): PostServiceImpl[F, DB] =
    new PostServiceImpl(postRepo, xa)
}
