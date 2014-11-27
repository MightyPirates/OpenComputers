package li.cil.oc.integration.buildcraft.tools

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModBuildCraftAPITools extends ModProxy {
  override def getMod = Mods.BuildCraftTools

  override def initialize(): Unit = {
    FMLInterModComms.sendMessage(Mods.IDs.OpenComputers, "registerWrenchTool", "li.cil.oc.integration.buildcraft.tools.EventHandlerBuildCraft.useWrench")
  }
}
