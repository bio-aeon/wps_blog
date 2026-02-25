package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.TestimonialResult
import su.wps.blog.services.TestimonialService

object TestimonialServiceMock {
  def create[F[_]: Applicative](
    result: List[TestimonialResult] = Nil
  ): TestimonialService[F] = new TestimonialService[F] {
    def getTestimonials: F[List[TestimonialResult]] = result.pure[F]
  }
}
