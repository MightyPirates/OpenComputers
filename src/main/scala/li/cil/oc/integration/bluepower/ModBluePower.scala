package li.cil.oc.integration.bluepower

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModBluePower extends ModProxy {
  override def getMod = Mods.BluePower

  override def initialize(): Unit = {
    RedstoneProvider.init()
  }
}
