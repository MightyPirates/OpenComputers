package li.cil.oc.integration.tcon

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.common.MinecraftForge

object ModTinkersConstruct extends ModProxy {
  override def getMod = Mods.TinkersConstruct

  override def initialize() {
    MinecraftForge.EVENT_BUS.register(EventHandlerTinkersConstruct)
  }
}
