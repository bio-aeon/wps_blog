package su.wps.blog.migrations

import su.wps.pgmigrations._

class Migrate_20181025220558_CreateConfig extends Migration {
  val tableName = "config"

  def up(): Unit = {
    createTable(tableName) { t =>
      t.integer("id", PrimaryKey, AutoIncrement)
      t.varchar("name", Limit(255), Unique, NotNull)
      t.varchar("value", Limit(255), NotNull)
      t.varchar("comment", Limit(1000), NotNull)
      t.timestamp("created_at", NotNull, Default("current_timestamp"))
    }
  }

  def down(): Unit = {
    dropTable(tableName)
  }
}
