package li.cil.oc.util

import java.io.IOException
import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.StandardCharsets
import scala.util.control.Breaks.break

import li.cil.oc.OpenComputers

object FontUtils {
  // Note: we load the widths from a file (one byte per width) because the Scala
  // compiler craps its pants when we try to have it as an array in the source
  // file... seems having an array with 0x10000 entries leads to stack overflows,
  // who would have known!
  private val widths = {
    val values: Array[Byte] = Array.fill[Byte](0x10000)(1)
    try {
      // Note to self: NOT VIA THE FUCKING RESOURCE SYSTEM BECAUSE IT'S FUCKING CLIENT ONLY YOU IDIOT.
      val font = FontUtils.getClass.getResourceAsStream("/assets/opencomputers/font.hex")
      try {
        val input = new BufferedReader(new InputStreamReader(font, StandardCharsets.UTF_8))
        while (true) {
          val line = input.readLine()
          if (line == null) break()
          val info = line.split(":")
          val charCode = Integer.parseInt(info(0), 16)
          if (charCode >= 0 && charCode < values.length) {
            if (info(1).length > 0 && info(1).length % 32 == 0) {
              values(charCode) = (info(1).length / 32).asInstanceOf[Byte]
            }
          }
        }
      } finally {
        try {
          font.close()
        } catch {
          case ex: IOException => OpenComputers.log.warn("Error parsing font.", ex)
          case _ => OpenComputers.log.warn("unknown exception")
        }
      }
    } catch {
      case ex: IOException => OpenComputers.log.warn("Failed loading glyphs.", ex)
      case _ => OpenComputers.log.warn("unknown exception")
    }
    values
  }

  def wcwidth(ch: Int) = if (ch < 0 || ch >= widths.length) -1 else widths(ch).asInstanceOf[Int]
}
