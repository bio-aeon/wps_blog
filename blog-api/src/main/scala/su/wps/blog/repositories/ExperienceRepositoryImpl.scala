package su.wps.blog.repositories

import su.wps.blog.models.domain.Experience
import su.wps.blog.repositories.sql.{ExperienceSql, ExperienceSqlImpl}
import tofu.doobie.LiftConnectionIO

final class ExperienceRepositoryImpl[DB[_]] private (sql: ExperienceSql[DB])
    extends ExperienceRepository[DB] {
  def findAllActive: DB[List[Experience]] = sql.findAllActive
}

object ExperienceRepositoryImpl {
  def create[DB[_]: LiftConnectionIO]: ExperienceRepositoryImpl[DB] = {
    val experienceSql = ExperienceSqlImpl.create[DB]
    new ExperienceRepositoryImpl[DB](experienceSql)
  }
}
