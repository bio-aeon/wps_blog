package su.wps.blog.services

import cats.Monad
import cats.syntax.functor.*
import io.scalaland.chimney.dsl.*
import mouse.anyf.*
import su.wps.blog.models.api.ExperienceResult
import su.wps.blog.repositories.ExperienceRepository
import tofu.doobie.transactor.Txr

final class ExperienceServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  experienceRepo: ExperienceRepository[DB],
  xa: Txr[F, DB]
) extends ExperienceService[F] {

  def getExperiences: F[List[ExperienceResult]] =
    experienceRepo.findAllActive.thrushK(xa.trans).map { experiences =>
      experiences.map(
        _.into[ExperienceResult].withFieldComputed(_.id, _.nonEmptyId).transform
      )
    }
}

object ExperienceServiceImpl {
  def create[F[_]: Monad, DB[_]: Monad](
    experienceRepo: ExperienceRepository[DB],
    xa: Txr[F, DB]
  ): ExperienceServiceImpl[F, DB] =
    new ExperienceServiceImpl(experienceRepo, xa)
}
