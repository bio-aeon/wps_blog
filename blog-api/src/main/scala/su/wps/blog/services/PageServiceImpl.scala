package su.wps.blog.services

import cats.Monad
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import mouse.anyf.*
import su.wps.blog.models.api.*
import su.wps.blog.models.domain.AppErr
import su.wps.blog.models.domain.AppErr.PageNotFound
import su.wps.blog.repositories.{PageRepository, PageTranslationRepository}
import tofu.Raise
import tofu.doobie.transactor.Txr

final class PageServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  pageRepo: PageRepository[DB],
  pageTranslationRepo: PageTranslationRepository[DB],
  xa: Txr[F, DB]
)(implicit R: Raise[F, AppErr])
    extends PageService[F] {
  import ServiceHelpers._

  def getPageByUrl(lang: String, url: String): F[PageResult] =
    pageRepo
      .findByUrl(url)
      .flatMap { (pageOpt: Option[su.wps.blog.models.domain.Page]) =>
        pageOpt match {
          case Some(page) =>
            pageTranslationRepo.findAvailableLanguages(page.nonEmptyId).map { availLangs =>
              Option(
                PageResult(
                  id = page.nonEmptyId.value,
                  url = page.url,
                  title = page.title,
                  content = Some(page.content),
                  createdAt = page.createdAt,
                  language = lang,
                  seo = None,
                  availableLanguages = if (availLangs.isEmpty) List(lang) else availLangs
                )
              )
            }
          case None =>
            Monad[DB].pure(Option.empty[PageResult])
        }
      }
      .thrushK(xa.trans)
      .flatMap {
        case Some(result) => result.pure[F]
        case None         => R.raise(PageNotFound(url))
      }

  def getAllPages(lang: String): F[ListItemsResult[ListPageResult]] =
    pageRepo.findAll.thrushK(xa.trans).map { pages =>
      val items = pages.map(p => ListPageResult(p.url, p.title, lang))
      ListItemsResult(items, items.size)
    }
}

object PageServiceImpl {
  def create[F[_]: Monad: Raise[*[_], AppErr], DB[_]: Monad](
    pageRepo: PageRepository[DB],
    pageTranslationRepo: PageTranslationRepository[DB],
    xa: Txr[F, DB]
  ): PageServiceImpl[F, DB] =
    new PageServiceImpl(pageRepo, pageTranslationRepo, xa)
}
