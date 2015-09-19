package li.cil.oc.integration.bluepower

import li.cil.oc.api
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModBluePower extends ModProxy {
  override def getMod = Mods.BluePower

  override def initialize(): Unit = {
    api.IMC.registerWrenchTool("li.cil.oc.integration.bluepower.EventHandlerBluePower.useWrench")
    api.IMC.registerWrenchToolCheck("li.cil.oc.integration.bluepower.EventHandlerBluePower.isWrench")

    RedstoneProvider.init()
  }
}
