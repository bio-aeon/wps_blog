package su.wps.blog.migrations

import su.wps.pgmigrations._

class Migrate_20180903221010_CreateUsers extends Migration {
  val tableName = "users"

  def up(): Unit =
    createTable(tableName) { t =>
      t.integer("id", PrimaryKey, AutoIncrement)
      t.varchar("username", Limit(255), NotNull, Unique)
      t.varchar("email", Limit(255), NotNull)
      t.varchar("password", Limit(255), NotNull)
      t.boolean("is_active", NotNull, Default("false"))
      t.boolean("is_admin", NotNull, Default("false"))
      t.timestamp("created_at", NotNull, Default("current_timestamp"))
    }

  def down(): Unit =
    dropTable(tableName)
}
