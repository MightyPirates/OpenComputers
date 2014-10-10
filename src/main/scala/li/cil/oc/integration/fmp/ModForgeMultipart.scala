package li.cil.oc.integration.fmp

import codechicken.multipart.MultiPartRegistry
import li.cil.oc.Settings
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.common.MinecraftForge

object ModForgeMultipart extends ModProxy {
  override def getMod = Mods.ForgeMultipart

  override def initialize() {
    MultiPartRegistry.registerConverter(MultipartConverter)
    MultiPartRegistry.registerParts(MultipartFactory, Array(Settings.namespace + "cable"))

    MinecraftForge.EVENT_BUS.register(EventHandler)
  }
}
