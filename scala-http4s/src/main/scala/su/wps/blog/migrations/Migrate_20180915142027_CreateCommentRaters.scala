package su.wps.blog.migrations

import su.wps.pgmigrations._

class Migrate_20180915142027_CreateCommentRaters extends Migration {
  val tableName = "comment_raters"

  def up(): Unit = {
    createTable(tableName) { t =>
      t.bigint("id", PrimaryKey, AutoIncrement)
      t.varchar("ip", Limit(39), NotNull)
      t.integer("comment_id", NotNull)
    }
  }

  def down(): Unit = {
    dropTable(tableName)
  }
}
