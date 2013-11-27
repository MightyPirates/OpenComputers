package li.cil.oc.util

import li.cil.oc.Settings
import net.minecraft.util.StatCollector
import org.lwjgl.input.Keyboard
import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

object Tooltip {
  def get(name: String, args: Any*): java.util.List[String] = {
    val tooltip = StatCollector.translateToLocal(Settings.namespace + "tooltip." + name).format(args.map(_.toString): _*)
    val isSubTooltip = name.contains(".")
    val shouldShorten = (isSubTooltip || tooltip.length > 50) &&
      !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) &&
      !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
    if (shouldShorten) {
      if (isSubTooltip) Seq.empty[String]
      else Seq(StatCollector.translateToLocal(Settings.namespace + "tooltip.TooLong"))
    }
    else {
      val nl = """\[nl\]"""
      val lines = mutable.ArrayBuffer.empty[String]
      tooltip.split(nl).foreach(line => {
        val formatted = line.trim.stripLineEnd
        var start = 0
        var end = 0
        var count = 0
        var formats = 0
        for (c <- formatted.trim) {
          if (c == '§') {
            formats += 1
          }
          else if (c == ' ') {
            end = count
          }
          count += 1
          if (count - formats > 45 && end > 0) {
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
}
