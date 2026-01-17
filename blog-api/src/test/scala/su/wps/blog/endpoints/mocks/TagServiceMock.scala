package su.wps.blog.endpoints.mocks

import cats.Applicative
import cats.syntax.applicative.*
import su.wps.blog.models.api.{ListItemsResult, TagCloudResult, TagWithCountResult}
import su.wps.blog.services.TagService

object TagServiceMock {
  def create[F[_]: Applicative](
    getAllTagsResult: List[TagWithCountResult] = Nil,
    getTagCloudResult: TagCloudResult = TagCloudResult(Nil)
  ): TagService[F] =
    new TagService[F] {
      def getAllTags: F[ListItemsResult[TagWithCountResult]] =
        ListItemsResult(getAllTagsResult, getAllTagsResult.length).pure[F]

      def getTagCloud: F[TagCloudResult] =
        getTagCloudResult.pure[F]
    }
}
