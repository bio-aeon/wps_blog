package su.wps.blog.migrations

class Migrate_20181027133823_CreateFKFromPostsTagsToPosts extends AddFkMigration {
  val tableName: String = "posts_tags"
  val fieldName: String = "post_id"
  val referenceName: String = "posts_tags_to_posts"
  val refFieldName: String = "id"
  val refTableName: String = "posts"
}
