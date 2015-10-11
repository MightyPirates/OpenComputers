package li.cil.oc.integration.buildcraft.tools

import li.cil.oc.api
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModBuildCraftAPITools extends ModProxy {
  override def getMod = Mods.BuildCraftTools

  override def initialize(): Unit = {
    api.IMC.registerWrenchTool("li.cil.oc.integration.buildcraft.tools.EventHandlerBuildCraft.useWrench")
    api.IMC.registerWrenchToolCheck("li.cil.oc.integration.buildcraft.tools.EventHandlerBuildCraft.isWrench")
  }
}
