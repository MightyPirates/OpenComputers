package li.cil.oc.integration.buildcraft.library

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModBuildCraftAPILibrary extends ModProxy {
  override def getMod = Mods.BuildCraftLibrary

  override def initialize(): Unit = {
    HandlerRegistry.init()
  }
}
