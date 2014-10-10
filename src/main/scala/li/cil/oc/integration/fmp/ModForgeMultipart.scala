package li.cil.oc.integration.fmp

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.common.MinecraftForge

object ModForgeMultipart extends ModProxy {
  override def getMod = Mods.ForgeMultipart

  override def initialize() {
    MultipartConverter.init()
    MultipartFactory.init()

    MinecraftForge.EVENT_BUS.register(EventHandler)
  }
}
