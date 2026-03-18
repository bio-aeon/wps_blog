package su.wps.blog.services

import cats.Monad
import cats.syntax.apply.*
import cats.syntax.functor.*
import mouse.anyf.*
import su.wps.blog.models.api.*
import su.wps.blog.models.domain.TagId
import su.wps.blog.repositories.{TagRepository, TagTranslationRepository}
import tofu.doobie.transactor.Txr

final class TagServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  tagRepo: TagRepository[DB],
  tagTranslationRepo: TagTranslationRepository[DB],
  xa: Txr[F, DB]
) extends TagService[F] {

  def getAllTags(lang: String): F[ListItemsResult[TagWithCountResult]] =
    (tagRepo.findAllWithPostCounts, tagTranslationRepo.findAllTranslatedNames(lang))
      .mapN((_, _))
      .thrushK(xa.trans)
      .map { case (tagsWithCounts, translatedNames) =>
        val items = tagsWithCounts.map { case (tag, count) =>
          val name = translatedNames.getOrElse(tag.nonEmptyId, tag.name)
          TagWithCountResult(tag.nonEmptyId, name, tag.slug, count)
        }
        ListItemsResult(items, items.length)
      }

  def getTagCloud(lang: String): F[TagCloudResult] =
    (tagRepo.findAllWithPostCounts, tagTranslationRepo.findAllTranslatedNames(lang))
      .mapN((_, _))
      .thrushK(xa.trans)
      .map { case (tagsWithCounts, translatedNames) =>
        val maxCount = tagsWithCounts.map(_._2).maxOption.getOrElse(1)
        val items = tagsWithCounts.map { case (tag, count) =>
          val name = translatedNames.getOrElse(tag.nonEmptyId, tag.name)
          TagCloudItem(name, tag.slug, count, count.toDouble / maxCount)
        }
        TagCloudResult(items)
      }
}

object TagServiceImpl {
  def create[F[_]: Monad, DB[_]: Monad](
    tagRepo: TagRepository[DB],
    tagTranslationRepo: TagTranslationRepository[DB],
    xa: Txr[F, DB]
  ): TagServiceImpl[F, DB] =
    new TagServiceImpl(tagRepo, tagTranslationRepo, xa)
}
