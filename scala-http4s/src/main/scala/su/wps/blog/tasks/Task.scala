package su.wps.blog.tasks

import java.io.File

import com.typesafe.config.ConfigFactory

class Task(argStr: String) {
  protected val args = argStr.split(" ").filter(_.nonEmpty)
  protected val config =
    ConfigFactory
      .parseFile(new File(getClass.getClassLoader.getResource("application.conf").getFile))
      .resolve()
}
