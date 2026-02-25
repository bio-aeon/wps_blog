package su.wps.blog.repositories

import su.wps.blog.models.domain.Testimonial
import su.wps.blog.repositories.sql.{TestimonialSql, TestimonialSqlImpl}
import tofu.doobie.LiftConnectionIO

final class TestimonialRepositoryImpl[DB[_]] private (sql: TestimonialSql[DB])
    extends TestimonialRepository[DB] {
  def findAllActive: DB[List[Testimonial]] = sql.findAllActive
}

object TestimonialRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: TestimonialRepositoryImpl[DB] = {
    val testimonialSql = TestimonialSqlImpl.create[DB]
    new TestimonialRepositoryImpl[DB](testimonialSql)
  }
}
