package li.cil.oc.util

import li.cil.oc.Settings
import net.minecraft.client.Minecraft
import net.minecraft.util.StatCollector
import org.lwjgl.input.Keyboard
import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

object Tooltip {
  val maxWidth = 200

  def get(name: String, args: Any*): java.util.List[String] = {
    val tooltip = StatCollector.translateToLocal(Settings.namespace + "tooltip." + name).format(args.map(_.toString): _*)
    val isSubTooltip = name.contains(".")
    val font = Minecraft.getMinecraft.fontRenderer
    val shouldShorten = (isSubTooltip || font.getStringWidth(tooltip) > maxWidth) &&
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
        var position = 0
        var start = 0
        var lineEnd = 0
        var width = 0
        var lineWidth = 0
        val iterator = formatted.iterator
        while (iterator.hasNext) {
          val c = iterator.next()
          if (c == '§') {
            iterator.next()
          }
          else {
            if (c == ' ') {
              lineEnd = position
              lineWidth = width
            }
            else {
              width += font.getCharWidth(c)
            }
            position += 1
            if (width > maxWidth) {
              if (lineEnd > start) {
                lines += formatted.substring(start, lineEnd)
                start = lineEnd + 1
                width -= lineWidth
                lineWidth = 0
              }
              else {
                lines += formatted.substring(start, position)
                start = position
                width = 0
              }
            }
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
