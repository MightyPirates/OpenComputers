package li.cil.oc.util

import java.io.IOException
import java.io.InputStream

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation

object FontUtils {
  // Note: we load the widths from a file (one byte per width) because the Scala
  // compiler craps its pants when we try to have it as an array in the source
  // file... seems having an array with 0x10000 entries leads to stack overflows,
  // who would have known!
  private val widths = {
    val ba = Array.fill[Byte](0x10000)(-1)
    Minecraft.getMinecraft.getResourceManager.getResource(new ResourceLocation(Settings.resourceDomain, "wcwidth.bin")).getInputStream match {
      case is: InputStream => try {
        is.read(ba)
        is.close()
      } catch {
        case e: IOException => OpenComputers.log.warn("Failed loading character widths. Font rendering will probably be derpy as all hell.", e)
      }
      case _ => // Null.
    }
    ba
  }

  def wcwidth(ch: Int) = if (ch < 0 || ch >= widths.length) -1 else widths(ch)
}
