package su.wps.blog.repositories

import doobie._
import doobie.implicits._
import su.wps.blog.models.Post

class PostRepositoryImpl extends DoobieRepository with PostRepository[ConnectionIO] {
  val tableName: Fragment = Fragment.const("auth_users")

  def findAllWithLimitAndOffset(limit: Int, offset: Int) =
    (fr"select name, short_text, text, author_id, views, meta_title, " ++
      fr"meta_keywords, meta_description, is_hidden, created_at, id from" ++ tableName ++
      fr"order by created_at desc limit $limit offset $offset")
      .query[Post]
      .to[List]

}
