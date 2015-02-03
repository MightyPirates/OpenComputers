package li.cil.oc.integration.waila

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.fml.common.event.FMLInterModComms

object ModWaila extends ModProxy {
  override def getMod = Mods.Waila

  override def initialize() {
    FMLInterModComms.sendMessage(Mods.IDs.Waila, "register", "li.cil.oc.integration.waila.BlockDataProvider.init")
  }
}
