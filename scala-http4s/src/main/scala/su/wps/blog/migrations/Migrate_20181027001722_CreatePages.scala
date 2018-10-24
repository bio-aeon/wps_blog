package su.wps.blog.migrations

import su.wps.pgmigrations._

class Migrate_20181027001722_CreatePages extends Migration {
  val tableName = "pages"

  def up(): Unit = {
    createTable(tableName) { t =>
      t.integer("id", PrimaryKey, AutoIncrement)
      t.varchar("url", Limit(100), NotNull)
      t.varchar("title", Limit(255), NotNull)
      t.varchar("content", Limit(1000), NotNull)
      t.timestamp("created_at", NotNull, Default("current_timestamp"))
    }
  }

  def down(): Unit = {
    dropTable(tableName)
  }
}
