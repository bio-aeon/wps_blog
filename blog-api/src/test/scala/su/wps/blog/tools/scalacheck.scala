package su.wps.blog.tools

import org.scalacheck.rng.Seed
import org.scalacheck.{Arbitrary, Gen}

import java.time.{Instant, ZoneId, ZonedDateTime}
import scala.reflect.runtime.universe.*
import scala.util.{Success, Try}

object scalacheck {
  final case class RandomDataException(msg: String) extends Exception(msg)

  implicit val arbZonedDt: Arbitrary[ZonedDateTime] = Arbitrary {
    Gen.chooseNum(10, 1000).map { secondsAgo =>
      val now = Instant.now.getEpochSecond
      ZonedDateTime.ofInstant(Instant.ofEpochSecond(now - secondsAgo), ZoneId.systemDefault)
    }
  }

  def random[T: WeakTypeTag: Gen]: T = random(1).head

  def random[T: WeakTypeTag](n: Int)(implicit gen: Gen[T]): List[T] = {
    val randomLong = scala.util.Random.nextLong()
    val streamGen = Gen.infiniteLazyList(gen)
    Try(streamGen.apply(Gen.Parameters.default, Seed(randomLong))) match {
      case Success(Some(x)) => x.take(n).toList
      case _ => raise[T]
    }
  }

  private def raise[T: WeakTypeTag]: Nothing = {
    val tpe = implicitly[WeakTypeTag[T]].tpe
    val msg = s"Could not generate a random value for $tpe."
    throw RandomDataException(msg)
  }
}
