package su.wps.blog.services

import cats.Monad
import cats.syntax.functor.*
import io.scalaland.chimney.dsl.*
import mouse.anyf.*
import su.wps.blog.models.api.TestimonialResult
import su.wps.blog.repositories.TestimonialRepository
import tofu.doobie.transactor.Txr

final class TestimonialServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  testimonialRepo: TestimonialRepository[DB],
  xa: Txr[F, DB]
) extends TestimonialService[F] {

  def getTestimonials: F[List[TestimonialResult]] =
    testimonialRepo.findAllActive.thrushK(xa.trans).map { testimonials =>
      testimonials.map(
        _.into[TestimonialResult].withFieldComputed(_.id, _.nonEmptyId).transform
      )
    }
}

object TestimonialServiceImpl {
  def create[F[_]: Monad, DB[_]: Monad](
    testimonialRepo: TestimonialRepository[DB],
    xa: Txr[F, DB]
  ): TestimonialServiceImpl[F, DB] =
    new TestimonialServiceImpl(testimonialRepo, xa)
}
