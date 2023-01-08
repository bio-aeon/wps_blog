package su.wps.blog.migrations

class Migrate_20181027132647_CreateFKFromPostsToUsers extends AddFkMigration {
  val tableName: String = "posts"
  val fieldName: String = "author_id"
  val referenceName: String = "post_to_user"
  val refFieldName: String = "id"
  val refTableName: String = "users"
}
