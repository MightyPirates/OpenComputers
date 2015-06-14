package li.cil.oc.integration.cofh.item

import li.cil.oc.api
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModCoFHItem extends ModProxy {
  override def getMod = Mods.CoFHItem

  override def initialize(): Unit = {
    api.IMC.registerWrenchTool("li.cil.oc.integration.cofh.item.EventHandlerCoFH.useWrench")
  }
}
