package li.cil.oc.integration.igw

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods

object ModIngameWiki extends ModProxy {
  override def getMod = Mods.IngameWiki

  override def initialize(): Unit = {
    WikiEventHandler.init()
  }
}
