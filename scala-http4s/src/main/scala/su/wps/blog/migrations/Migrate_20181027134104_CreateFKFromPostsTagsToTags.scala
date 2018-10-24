package su.wps.blog.migrations

class Migrate_20181027134104_CreateFKFromPostsTagsToTags extends AddFkMigration {
  val tableName: String = "posts_tags"
  val fieldName: String = "tag_id"
  val referenceName: String = "posts_tags_to_tags"
  val refFieldName: String = "id"
  val refTableName: String = "tags"
}
