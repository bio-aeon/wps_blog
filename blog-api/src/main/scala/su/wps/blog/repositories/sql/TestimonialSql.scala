package su.wps.blog.repositories.sql

import derevo.derive
import su.wps.blog.models.domain.Testimonial
import tofu.higherKind.derived.representableK

@derive(representableK)
trait TestimonialSql[DB[_]] {
  def findAllActive: DB[List[Testimonial]]
}
