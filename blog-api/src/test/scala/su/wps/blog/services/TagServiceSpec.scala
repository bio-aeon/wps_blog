package su.wps.blog.services

import cats.Id
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.{Tag, TagId}
import su.wps.blog.services.mocks.{TagRepositoryMock, TxrMock}
import tofu.doobie.transactor.Txr

class TagServiceSpec extends Specification {
  type RunF[A] = Either[Throwable, A]

  val xa: Txr[RunF, Id] = TxrMock.create[RunF]

  "TagService should" >> {
    "return all tags with post counts" >> {
      val tags = List(
        Tag("scala", "scala", Some(TagId(1))),
        Tag("rust", "rust", Some(TagId(2))),
        Tag("fp", "fp", Some(TagId(3)))
      )
      val tagsWithCounts = tags.map(t => (t, 5))
      val service = mkService(tagsWithCounts)

      service.getAllTags must beRight.which { r =>
        r.items.length == 3 && r.total == 3
      }
    }

    "return tags with correct post counts" >> {
      val tags = List(
        (Tag("scala", "scala", Some(TagId(1))), 10),
        (Tag("rust", "rust", Some(TagId(2))), 5),
        (Tag("fp", "fp", Some(TagId(3))), 0)
      )
      val service = mkService(tags)

      service.getAllTags must beRight.which { r =>
        r.items.find(_.slug == "scala").exists(_.postCount == 10) &&
        r.items.find(_.slug == "rust").exists(_.postCount == 5) &&
        r.items.find(_.slug == "fp").exists(_.postCount == 0)
      }
    }

    "return empty list when no tags exist" >> {
      val service = mkService(Nil)

      service.getAllTags must beRight.which { r =>
        r.items.isEmpty && r.total == 0
      }
    }

    "return tags ordered by name (as provided by repository)" >> {
      val tags = List(
        (Tag("alpha", "alpha", Some(TagId(1))), 3),
        (Tag("beta", "beta", Some(TagId(2))), 2),
        (Tag("gamma", "gamma", Some(TagId(3))), 1)
      )
      val service = mkService(tags)

      service.getAllTags must beRight.which { r =>
        r.items.map(_.name) == List("alpha", "beta", "gamma")
      }
    }

    "transform Tag domain model to TagWithCountResult API model" >> {
      val tag = Tag("scala", "scala-lang", Some(TagId(42)))
      val service = mkService(List((tag, 7)))

      service.getAllTags must beRight.which { r =>
        r.items.headOption.exists { t =>
          t.id == TagId(42) &&
          t.name == "scala" &&
          t.slug == "scala-lang" &&
          t.postCount == 7
        }
      }
    }
  }

  private def mkService(
    findAllWithPostCountsResult: List[(Tag, Int)] = Nil
  ): TagService[RunF] = {
    val tagRepo = TagRepositoryMock.create[Id](
      findAllWithPostCountsResult = findAllWithPostCountsResult
    )
    TagServiceImpl.create[RunF, Id](tagRepo, xa)
  }
}
