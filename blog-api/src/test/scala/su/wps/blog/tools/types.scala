package su.wps.blog.tools

import doobie.Meta
import org.scalacheck.{Arbitrary, Gen}
import shapeless.Witness

object types {
  final case class PosInt(value: Int)

  object PosInt {
    implicit val gen: Gen[PosInt] = Gen.posNum[Int].map(PosInt.apply)

    implicit def arb: Arbitrary[PosInt] = Arbitrary(gen)

    implicit val meta: Meta[PosInt] = Meta[Int].imap(PosInt(_))(_.value)
  }

  final case class Varchar[I <: Int: Witness.Aux](value: String)

  object Varchar {

    implicit def arb[I <: Int](implicit ws: Witness.Aux[I]): Arbitrary[Varchar[I]] =
      Arbitrary(
        Gen
          .chooseNum(1, ws.value)
          .flatMap(Gen.listOfN(_, Gen.alphaNumChar))
          .map(x => Varchar(x.mkString))
      )

    implicit def meta[I <: Int](implicit ws: Witness.Aux[I]): Meta[Varchar[I]] =
      Meta[String].imap(Varchar[I](_))(_.value)
  }

  val W: Witness.type = shapeless.Witness
}
