package su.wps.blog.services

import cats.Monad
import cats.syntax.functor.*
import io.scalaland.chimney.dsl.*
import mouse.anyf.*
import su.wps.blog.models.api.{ListItemsResult, TagCloudItem, TagCloudResult, TagWithCountResult}
import su.wps.blog.repositories.TagRepository
import tofu.doobie.transactor.Txr

final class TagServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  tagRepo: TagRepository[DB],
  xa: Txr[F, DB]
) extends TagService[F] {

  def getAllTags: F[ListItemsResult[TagWithCountResult]] =
    tagRepo.findAllWithPostCounts
      .thrushK(xa.trans)
      .map { tagsWithCounts =>
        val items = tagsWithCounts.map { case (tag, count) =>
          tag
            .into[TagWithCountResult]
            .withFieldComputed(_.id, _.nonEmptyId)
            .withFieldConst(_.postCount, count)
            .transform
        }
        ListItemsResult(items, items.length)
      }

  def getTagCloud: F[TagCloudResult] =
    tagRepo.findAllWithPostCounts
      .thrushK(xa.trans)
      .map { tagsWithCounts =>
        val maxCount = tagsWithCounts.map(_._2).maxOption.getOrElse(1)
        val items = tagsWithCounts.map { case (tag, count) =>
          tag
            .into[TagCloudItem]
            .withFieldConst(_.count, count)
            .withFieldConst(_.weight, count.toDouble / maxCount)
            .transform
        }
        TagCloudResult(items)
      }
}

object TagServiceImpl {
  def create[F[_]: Monad, DB[_]: Monad](
    tagRepo: TagRepository[DB],
    xa: Txr[F, DB]
  ): TagServiceImpl[F, DB] =
    new TagServiceImpl(tagRepo, xa)
}
