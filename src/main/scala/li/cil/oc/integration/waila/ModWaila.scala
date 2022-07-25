package li.cil.oc.integration.waila

import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import net.minecraftforge.fml.InterModComms

object ModWaila extends ModProxy {
  override def getMod = Mods.Waila

  override def initialize() = Unit
}
