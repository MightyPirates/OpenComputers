package li.cil.oc.integration.re

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.common.MinecraftForge

object ModResonantEngine extends ModProxy {
  override def getMod = Mods.ResonantEngine

  override def initialize() {
    MinecraftForge.EVENT_BUS.register(EventHandlerResonantEngine)
  }
}
