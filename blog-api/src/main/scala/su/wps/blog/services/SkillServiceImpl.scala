package su.wps.blog.services

import cats.Monad
import cats.syntax.functor.*
import io.scalaland.chimney.dsl.*
import mouse.anyf.*
import su.wps.blog.models.api.{SkillCategoryResult, SkillResult}
import su.wps.blog.repositories.SkillRepository
import tofu.doobie.transactor.Txr

final class SkillServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  skillRepo: SkillRepository[DB],
  xa: Txr[F, DB]
) extends SkillService[F] {

  def getSkillsByCategory: F[List[SkillCategoryResult]] =
    skillRepo.findAllActive.thrushK(xa.trans).map { skills =>
      skills
        .groupBy(_.category)
        .toList
        .sortBy(_._1)
        .map { case (category, categorySkills) =>
          SkillCategoryResult(
            category,
            categorySkills
              .sortBy(_.sortOrder)
              .map(_.into[SkillResult].withFieldComputed(_.id, _.nonEmptyId).transform)
          )
        }
    }
}

object SkillServiceImpl {
  def create[F[_]: Monad, DB[_]: Monad](
    skillRepo: SkillRepository[DB],
    xa: Txr[F, DB]
  ): SkillServiceImpl[F, DB] =
    new SkillServiceImpl(skillRepo, xa)
}
