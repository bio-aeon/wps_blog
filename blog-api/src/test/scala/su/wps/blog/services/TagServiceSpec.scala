package su.wps.blog.services

import cats.Id
import org.specs2.mutable.Specification
import su.wps.blog.models.domain.{Tag, TagId}
import su.wps.blog.services.mocks.*
import tofu.doobie.transactor.Txr

class TagServiceSpec extends Specification {
  type RunF[A] = Either[Throwable, A]

  val xa: Txr[RunF, Id] = TxrMock.create[RunF]

  "TagService" >> {
    "returns all tags with post counts" >> {
      val tags = List(
        Tag("scala", "scala", Some(TagId(1))),
        Tag("rust", "rust", Some(TagId(2))),
        Tag("fp", "fp", Some(TagId(3)))
      )
      val tagsWithCounts = tags.map(t => (t, 5))
      val service = mkService(tagsWithCounts)

      service.getAllTags("en") must beRight.which { r =>
        r.items.length == 3 && r.total == 3
      }
    }

    "returns tags with correct post counts" >> {
      val tags = List(
        (Tag("scala", "scala", Some(TagId(1))), 10),
        (Tag("rust", "rust", Some(TagId(2))), 5),
        (Tag("fp", "fp", Some(TagId(3))), 0)
      )
      val service = mkService(tags)

      service.getAllTags("en") must beRight.which { r =>
        r.items.find(_.slug == "scala").exists(_.postCount == 10) &&
        r.items.find(_.slug == "rust").exists(_.postCount == 5) &&
        r.items.find(_.slug == "fp").exists(_.postCount == 0)
      }
    }

    "returns empty list when no tags exist" >> {
      val service = mkService(Nil)

      service.getAllTags("en") must beRight.which { r =>
        r.items.isEmpty && r.total == 0
      }
    }

    "returns tags ordered by name (as provided by repository)" >> {
      val tags = List(
        (Tag("alpha", "alpha", Some(TagId(1))), 3),
        (Tag("beta", "beta", Some(TagId(2))), 2),
        (Tag("gamma", "gamma", Some(TagId(3))), 1)
      )
      val service = mkService(tags)

      service.getAllTags("en") must beRight.which { r =>
        r.items.map(_.name) == List("alpha", "beta", "gamma")
      }
    }

    "transforms Tag domain model to TagWithCountResult API model" >> {
      val tag = Tag("scala", "scala-lang", Some(TagId(42)))
      val service = mkService(List((tag, 7)))

      service.getAllTags("en") must beRight.which { r =>
        r.items.headOption.exists { t =>
          t.id == TagId(42) &&
          t.name == "scala" &&
          t.slug == "scala-lang" &&
          t.postCount == 7
        }
      }
    }
  }

  "TagService.getTagCloud" >> {
    "returns tag cloud with normalized weights" >> {
      val tags = List(
        (Tag("scala", "scala", Some(TagId(1))), 10),
        (Tag("rust", "rust", Some(TagId(2))), 5),
        (Tag("fp", "fp", Some(TagId(3))), 2)
      )
      val service = mkService(tags)

      service.getTagCloud("en") must beRight.which { r =>
        r.tags.find(_.slug == "scala").exists(_.weight == 1.0) &&
        r.tags.find(_.slug == "rust").exists(_.weight == 0.5) &&
        r.tags.find(_.slug == "fp").exists(_.weight == 0.2)
      }
    }

    "returns empty tag cloud when no tags exist" >> {
      val service = mkService(Nil)

      service.getTagCloud("en") must beRight.which { r =>
        r.tags.isEmpty
      }
    }

    "returns weight of 1.0 for single tag" >> {
      val tags = List((Tag("scala", "scala", Some(TagId(1))), 5))
      val service = mkService(tags)

      service.getTagCloud("en") must beRight.which { r =>
        r.tags.length == 1 && r.tags.head.weight == 1.0
      }
    }

    "returns correct counts in tag cloud items" >> {
      val tags =
        List((Tag("scala", "scala", Some(TagId(1))), 10), (Tag("rust", "rust", Some(TagId(2))), 5))
      val service = mkService(tags)

      service.getTagCloud("en") must beRight.which { r =>
        r.tags.find(_.slug == "scala").exists(_.count == 10) &&
        r.tags.find(_.slug == "rust").exists(_.count == 5)
      }
    }

    "handles tags with zero post count" >> {
      val tags =
        List((Tag("scala", "scala", Some(TagId(1))), 10), (Tag("rust", "rust", Some(TagId(2))), 0))
      val service = mkService(tags)

      service.getTagCloud("en") must beRight.which { r =>
        r.tags.find(_.slug == "rust").exists(_.weight == 0.0) &&
        r.tags.find(_.slug == "rust").exists(_.count == 0)
      }
    }
  }

  private def mkService(findAllWithPostCountsResult: List[(Tag, Int)] = Nil): TagService[RunF] = {
    val tagRepo =
      TagRepositoryMock.create[Id](findAllWithPostCountsResult = findAllWithPostCountsResult)
    val tagTranslationRepo = TagTranslationRepositoryMock.create[Id]()
    TagServiceImpl.create[RunF, Id](tagRepo, tagTranslationRepo, xa)
  }
}
