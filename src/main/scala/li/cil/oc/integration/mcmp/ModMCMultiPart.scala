package li.cil.oc.integration.mcmp

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModMCMultiPart extends ModProxy {
  override def getMod = Mods.MCMultiPart

  override def initialize(): Unit = {
    MCMultiPart.init()
  }
}
