package su.wps.blog.services.mocks

import cats.syntax.applicative.*
import cats.{Applicative, Id, ~>}
import fs2.Stream
import tofu.doobie.transactor.Txr

object TxrMock {
  def create[F[_]: Applicative]: Txr[F, Id] = new Txr[F, Id] {
    def trans: Id ~> F = λ[Id ~> F](_.pure[F])

    def rawTrans: Id ~> F = λ[Id ~> F](_.pure[F])

    def transP: Stream[Id, *] ~> Stream[F, *] =
      λ[Stream[Id, *] ~> Stream[F, *]](_.translate(λ[Id ~> F](_.pure[F])))

    def rawTransP: Stream[Id, *] ~> Stream[F, *] =
      λ[Stream[Id, *] ~> Stream[F, *]](_.translate(λ[Id ~> F](_.pure[F])))
  }
}
