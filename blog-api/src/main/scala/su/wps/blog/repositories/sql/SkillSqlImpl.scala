package su.wps.blog.repositories.sql

import cats.tagless.syntax.functorK.*
import doobie.*
import doobie.implicits.*
import su.wps.blog.instances.time.*
import su.wps.blog.models.domain.Skill
import tofu.doobie.LiftConnectionIO

final class SkillSqlImpl private extends SkillSql[ConnectionIO] {

  def findAllActive: ConnectionIO[List[Skill]] =
    sql"""SELECT name, slug, category, proficiency, icon, sort_order,
                 is_active, created_at, id
          FROM skills
          WHERE is_active = true
          ORDER BY sort_order, name"""
      .query[Skill]
      .to[List]
}

object SkillSqlImpl {
  def create[DB[_]](implicit L: LiftConnectionIO[DB]): SkillSql[DB] =
    new SkillSqlImpl().mapK(L.liftF)
}
