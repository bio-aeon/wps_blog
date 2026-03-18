package su.wps.blog.repositories

object LanguageMapping {
  val FtsConfigs: Map[String, String] = Map(
    "en" -> "english",
    "ru" -> "russian",
    "el" -> "greek"
  )

  def ftsConfig(lang: String): String =
    FtsConfigs.getOrElse(lang, "simple")
}
