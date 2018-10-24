package su.wps.blog.migrations

import su.wps.pgmigrations._

class Migrate_20180914224057_CreateComments extends Migration {
  val tableName = "comments"

  def up(): Unit = {
    createTable(tableName) { t =>
      t.integer("id", PrimaryKey, AutoIncrement)
      t.varchar("text", Limit(1000), NotNull)
      t.varchar("name", Limit(255), NotNull)
      t.varchar("email", Limit(75), NotNull)
      t.integer("post_id", NotNull)
      t.integer("parent_id")
      t.integer("left", Unsigned, NotNull)
      t.integer("right", Unsigned, NotNull)
      t.integer("tree_id", Unsigned, NotNull)
      t.integer("level", Unsigned, NotNull)
      t.integer("rating", NotNull)
      t.timestamp("created_at", NotNull, Default("current_timestamp"))
    }
  }

  def down(): Unit = {
    dropTable(tableName)
  }
}
