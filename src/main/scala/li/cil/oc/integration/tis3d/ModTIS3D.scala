package li.cil.oc.integration.tis3d

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModTIS3D extends ModProxy {
  override def getMod = Mods.TIS3D

  override def initialize(): Unit = {
    SerialInterfaceProviderAdapter.init()
  }
}
