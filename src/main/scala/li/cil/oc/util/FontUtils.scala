package li.cil.oc.util

import java.io.IOException

import li.cil.oc.OpenComputers

object FontUtils {
  // Note: we load the widths from a file (one byte per width) because the Scala
  // compiler craps its pants when we try to have it as an array in the source
  // file... seems having an array with 0x10000 entries leads to stack overflows,
  // who would have known!
  private val widths = {
    val ba = Array.fill[Byte](0x10000)(-1)
    // Note to self: NOT VIA THE FUCKING RESOURCE SYSTEM BECAUSE IT'S FUCKING CLIENT ONLY YOU IDIOT.
    val is = FontUtils.getClass.getResourceAsStream("/assets/opencomputers/wcwidth.bin")
    if (is != null) {
      try {
        is.read(ba)
        is.close()
      } catch {
        case e: IOException => OpenComputers.log.warn("Failed loading character widths. Font rendering will probably be derpy as all hell.", e)
      }
    }
    ba
  }

  def wcwidth(ch: Int): Int = if (ch < 0 || ch >= widths.length) -1 else widths(ch)
}
