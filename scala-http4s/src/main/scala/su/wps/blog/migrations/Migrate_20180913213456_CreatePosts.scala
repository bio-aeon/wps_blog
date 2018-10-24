package su.wps.blog.migrations

import su.wps.pgmigrations._

class Migrate_20180913213456_CreatePosts extends Migration {
  val tableName = "posts"

  def up(): Unit = {
    createTable(tableName) { t =>
      t.integer("id", PrimaryKey, AutoIncrement)
      t.varchar("name", Limit(255), NotNull)
      t.varchar("short_text", Limit(1000), NotNull)
      t.varchar("text", Limit(1000), NotNull)
      t.integer("author_id", NotNull)
      t.integer("views", NotNull)
      t.varchar("meta_title", Limit(255), NotNull)
      t.varchar("meta_keywords", Limit(255), NotNull)
      t.varchar("meta_description", Limit(255), NotNull)
      t.boolean("hidden", NotNull)
      t.timestamp("created_at", NotNull, Default("current_timestamp"))
    }
  }

  def down(): Unit = {
    dropTable(tableName)
  }
}
