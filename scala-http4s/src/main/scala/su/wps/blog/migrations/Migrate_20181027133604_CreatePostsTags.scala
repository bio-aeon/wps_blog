package su.wps.blog.migrations

import su.wps.pgmigrations._

class Migrate_20181027133604_CreatePostsTags extends Migration {
  val tableName = "posts_tags"

  def up() {
    createTable(tableName) { t =>
      t.integer("post_id", NotNull)
      t.integer("tag_id", NotNull)
      t.timestamp("created_at", NotNull, Default("current_timestamp"))
    }
  }

  def down() {
    dropTable(tableName)
  }
}
