package su.wps.blog.repositories

import su.wps.blog.models.domain.Skill
import su.wps.blog.repositories.sql.{SkillSql, SkillSqlImpl}
import tofu.doobie.LiftConnectionIO

final class SkillRepositoryImpl[DB[_]] private (sql: SkillSql[DB])
    extends SkillRepository[DB] {
  def findAllActive: DB[List[Skill]] = sql.findAllActive
}

object SkillRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: SkillRepositoryImpl[DB] = {
    val skillSql = SkillSqlImpl.create[DB]
    new SkillRepositoryImpl[DB](skillSql)
  }
}
