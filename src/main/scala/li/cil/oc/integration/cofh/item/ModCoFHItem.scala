package li.cil.oc.integration.cofh.item

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModCoFHItem extends ModProxy {
  override def getMod = Mods.CoFHItem

  override def initialize(): Unit = {
    FMLInterModComms.sendMessage(Mods.IDs.OpenComputers, "registerWrenchTool", "li.cil.oc.integration.cofh.item.EventHandlerCoFH.useWrench")
  }
}
