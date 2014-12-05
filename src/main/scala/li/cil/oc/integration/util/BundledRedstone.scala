package li.cil.oc.integration.util

import li.cil.oc.integration.Mods

object BundledRedstone {
  def isAvailable = Mods.RedLogic.isAvailable ||
    Mods.MineFactoryReloaded.isAvailable ||
    Mods.ProjectRedTransmission.isAvailable
}
