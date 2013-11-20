package li.cil.oc.util

import net.minecraftforge.common.Configuration
import scala.language.implicitConversions

object ExtendedConfiguration {
  implicit def extendedConfiguration(config: Configuration) = new ExtendedConfiguration(config)

  class ExtendedConfiguration(config: Configuration) {
    def fetch(path: String, default: Boolean, comment: String) = {
      val (category, name) = parse(path)
      config.get(category, name, default, wrapComment(category, comment)).getBoolean(default)
    }

    def fetch(path: String, default: Int, comment: String) = {
      val (category, name) = parse(path)
      config.get(category, name, default, wrapComment(category, comment)).getInt(default)
    }

    def fetch(path: String, default: Float, comment: String) = {
      val (category, name) = parse(path)
      config.get(category, name, default, wrapComment(category, comment)).getDouble(default).toFloat
    }

    def fetch(path: String, default: Double, comment: String) = {
      val (category, name) = parse(path)
      config.get(category, name, default, wrapComment(category, comment)).getDouble(default)
    }

    def fetch(path: String, default: String, comment: String) = {
      val (category, name) = parse(path)
      config.get(category, name, default, wrapComment(category, comment)).getString
    }

    private def parse(path: String) = {
      val (category, name) = path.splitAt(path.lastIndexOf("."))
      (category, name.substring(1))
    }

    private def wrapComment(category: String, comment: String) = {
      val indent = 1 + category.count(_ == '.')
      val wrapRegEx = ("""(.{1,""" + (78 - indent * 4 - 2) + """})(\s|\z)""").r
      val cleaned = comment.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").replace("[nl]", "\n").trim()
      wrapRegEx.replaceAllIn(cleaned, m => m.group(1).trim() + "\n").stripLineEnd
    }
  }

}
