package su.wps.blog.repositories.sql

import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import su.wps.blog.instances.time.*
import su.wps.blog.models.domain.Experience
import tofu.doobie.LiftConnectionIO

final class ExperienceSqlImpl private extends ExperienceSql[ConnectionIO] {

  def findAllActive: ConnectionIO[List[Experience]] =
    sql"""SELECT company, position, description, start_date, end_date,
                 location, company_url, sort_order, is_active, created_at, id
          FROM experiences
          WHERE is_active = true
          ORDER BY sort_order, start_date DESC"""
      .query[Experience]
      .to[List]
}

object ExperienceSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): ExperienceSql[DB] =
    new ExperienceSqlImpl().mapK(L.liftF)
}
