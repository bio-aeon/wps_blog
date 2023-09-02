package su.wps.blog

import cats.effect._

object App extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Program.resource[IO].use(_ => IO.never).as(ExitCode.Success)
}
