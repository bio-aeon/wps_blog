package su.wps.blog.services

import cats.Monad
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import io.scalaland.chimney.dsl.*
import mouse.anyf.*
import su.wps.blog.models.api.{ListItemsResult, ListPageResult, PageResult}
import su.wps.blog.models.domain.AppErr
import su.wps.blog.models.domain.AppErr.PageNotFound
import su.wps.blog.repositories.PageRepository
import tofu.Raise
import tofu.doobie.transactor.Txr

final class PageServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  pageRepo: PageRepository[DB],
  xa: Txr[F, DB]
)(implicit R: Raise[F, AppErr])
    extends PageService[F] {

  def getPageByUrl(url: String): F[PageResult] =
    pageRepo
      .findByUrl(url)
      .thrushK(xa.trans)
      .flatMap {
        case Some(page) =>
          page
            .into[PageResult]
            .withFieldComputed(_.id, _.nonEmptyId.value)
            .transform
            .pure[F]
        case None =>
          R.raise(PageNotFound(url))
      }

  def getAllPages: F[ListItemsResult[ListPageResult]] =
    pageRepo.findAll.thrushK(xa.trans).map { pages =>
      val items = pages.map(_.into[ListPageResult].transform)
      ListItemsResult(items, items.size)
    }
}

object PageServiceImpl {
  def create[F[_]: Monad: Raise[*[_], AppErr], DB[_]: Monad](
    pageRepo: PageRepository[DB],
    xa: Txr[F, DB]
  ): PageServiceImpl[F, DB] =
    new PageServiceImpl(pageRepo, xa)
}
