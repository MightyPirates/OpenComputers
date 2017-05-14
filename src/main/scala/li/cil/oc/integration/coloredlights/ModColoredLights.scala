package li.cil.oc.integration.coloredlights

/* TODO Colored Lights
import coloredlightscore.src.api.CLApi
*/
import li.cil.oc.integration.Mods
import net.minecraft.block.Block

// Doesn't need initialization, just a thin wrapper for block light value initialization.
object ModColoredLights {
  def setLightLevel(block: Block, r: Int, g: Int, b: Int): Unit = {
    // Extra layer of indirection because I've learned to be paranoid when it comes to class loading...
    if (Mods.ColoredLights.isModAvailable)
      setColoredLightLevel(block, r, g, b)
    else
      setPlainLightLevel(block, r, g, b)
  }

  private def setColoredLightLevel(block: Block, r: Int, g: Int, b: Int): Unit = {
/* TODO Colored Lights
    CLApi.setBlockColorRGB(block, r, g, b)
*/
  }

  private def setPlainLightLevel(block: Block, r: Int, g: Int, b: Int): Unit = {
    val brightness = Array(r, g, b).max
    block.setLightLevel((brightness + 0.1f) / 15f)
  }
}
