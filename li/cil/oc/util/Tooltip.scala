package li.cil.oc.util

import li.cil.oc.Config
import net.minecraft.util.StatCollector
import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

object Tooltip {
  def get(name: String, args: Any*): java.util.List[String] = {
    val tooltip = StatCollector.translateToLocal(Config.namespace + "tooltip." + name).format(args.map(_.toString): _*)
    val regex = """(\[[0123456789abcdefklmnor]\])""".r
    val nl = """\[nl\]"""
    val lines = mutable.ArrayBuffer.empty[String]
    tooltip.split(nl).foreach(line => {
      val formatted = regex.replaceAllIn(line.trim, m => "\u00A7" + m.group(1).charAt(1)).stripLineEnd
      var start = 0
      var end = 0
      var count = 0
      var formats = 0
      for (c <- formatted.trim) {
        if (c == '\u00A7') {
          formats += 1
        }
        else if (c == ' ') {
          end = count
        }
        count += 1
        if (count - formats > 50 && end > 0) {
          lines += formatted.substring(start, start + end)
          count -= end + 1
          start += end + 1
          end = 0
        }
      }
      if (start < formatted.length) {
        lines += formatted.substring(start)
      }
    })
    lines
  }
}
