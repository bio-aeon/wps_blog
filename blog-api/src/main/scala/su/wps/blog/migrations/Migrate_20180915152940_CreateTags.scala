package su.wps.blog.migrations

import su.wps.pgmigrations._

class Migrate_20180915152940_CreateTags extends Migration {
  val tableName = "tags"

  def up(): Unit =
    createTable(tableName) { t =>
      t.integer("id", PrimaryKey, AutoIncrement)
      t.varchar("name", Limit(100), NotNull)
      t.varchar("slug", Limit(100), Unique, NotNull)
    }

  def down(): Unit =
    dropTable(tableName)
}
