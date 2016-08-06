package li.cil.oc.integration.tesla

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModTesla extends ModProxy {
  override def getMod = Mods.Tesla

  override def initialize(): Unit = {
    Tesla.init()
  }
}
