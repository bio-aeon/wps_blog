package su.wps.blog.services

import cats.Id
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.AppErr.PageNotFound
import su.wps.blog.models.domain.{Page, PageId}
import su.wps.blog.services.mocks.{PageRepositoryMock, TxrMock}
import tofu.doobie.transactor.Txr

import java.time.ZonedDateTime

class PageServiceSpec extends Specification {
  type RunF[A] = Either[Throwable, A]

  val xa: Txr[RunF, Id] = TxrMock.create[RunF]

  "PageService.getPageByUrl should" >> {
    "return page when found" >> {
      val page = Page("about", "About Us", "About content", ZonedDateTime.now(), Some(PageId(1)))
      val service = mkService(Some(page))

      service.getPageByUrl("about") must beRight.which { r =>
        r.url == "about" && r.title == "About Us" && r.id == 1
      }
    }

    "raise PageNotFound error when page does not exist" >> {
      val service = mkService(None)

      service.getPageByUrl("non-existent") must beLeft.which {
        case PageNotFound(url) => url == "non-existent"
        case _                 => false
      }
    }

    "transform Page domain model to PageResult API model" >> {
      val createdAt = ZonedDateTime.now()
      val page = Page("contact", "Contact", "Contact us here", createdAt, Some(PageId(42)))
      val service = mkService(Some(page))

      service.getPageByUrl("contact") must beRight.which { r =>
        r.id == 42 &&
        r.url == "contact" &&
        r.title == "Contact" &&
        r.content == "Contact us here" &&
        r.createdAt == createdAt
      }
    }
  }

  "PageService.getAllPages should" >> {
    "return all pages as ListPageResult items" >> {
      val pages = List(
        Page("about", "About Us", "About content", ZonedDateTime.now(), Some(PageId(1))),
        Page("contact", "Contact", "Contact content", ZonedDateTime.now(), Some(PageId(2)))
      )
      val service = mkService(findAllResult = pages)

      service.getAllPages must beRight.which { r =>
        r.items.size == 2 &&
        r.total == 2 &&
        r.items.map(_.url) == List("about", "contact")
      }
    }

    "return empty list when no pages exist" >> {
      val service = mkService(findAllResult = Nil)

      service.getAllPages must beRight.which { r =>
        r.items.isEmpty && r.total == 0
      }
    }

    "transform Page domain to ListPageResult with url and title only" >> {
      val pages = List(
        Page("privacy", "Privacy Policy", "Long privacy content", ZonedDateTime.now(), Some(PageId(1)))
      )
      val service = mkService(findAllResult = pages)

      service.getAllPages must beRight.which { r =>
        r.items.head.url == "privacy" &&
        r.items.head.title == "Privacy Policy"
      }
    }
  }

  private def mkService(
    findByUrlResult: Option[Page] = None,
    findAllResult: List[Page] = Nil
  ): PageService[RunF] = {
    val pageRepo = PageRepositoryMock.create[Id](
      findByUrlResult = findByUrlResult,
      findAllResult = findAllResult
    )
    PageServiceImpl.create[RunF, Id](pageRepo, xa)
  }
}
