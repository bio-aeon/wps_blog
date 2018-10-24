package su.wps

import cats.~>

import scala.concurrent.Future

package object blog {
  object data {
    type LiftFuture[F[_]] = Future ~> F
  }
}
