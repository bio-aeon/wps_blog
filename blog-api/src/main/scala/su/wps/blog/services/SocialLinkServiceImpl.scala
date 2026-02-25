package su.wps.blog.services

import cats.Monad
import cats.syntax.functor.*
import io.scalaland.chimney.dsl.*
import mouse.anyf.*
import su.wps.blog.models.api.SocialLinkResult
import su.wps.blog.repositories.SocialLinkRepository
import tofu.doobie.transactor.Txr

final class SocialLinkServiceImpl[F[_]: Monad, DB[_]: Monad] private (
  socialLinkRepo: SocialLinkRepository[DB],
  xa: Txr[F, DB]
) extends SocialLinkService[F] {

  def getSocialLinks: F[List[SocialLinkResult]] =
    socialLinkRepo.findAllActive.thrushK(xa.trans).map { links =>
      links.map(
        _.into[SocialLinkResult].withFieldComputed(_.id, _.nonEmptyId).transform
      )
    }
}

object SocialLinkServiceImpl {
  def create[F[_]: Monad, DB[_]: Monad](
    socialLinkRepo: SocialLinkRepository[DB],
    xa: Txr[F, DB]
  ): SocialLinkServiceImpl[F, DB] =
    new SocialLinkServiceImpl(socialLinkRepo, xa)
}
