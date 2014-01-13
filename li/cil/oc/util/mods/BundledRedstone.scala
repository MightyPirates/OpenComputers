package li.cil.oc.util.mods

import cpw.mods.fml.common.Loader

object BundledRedstone {
  def isAvailable = Loader.isModLoaded("RedLogic") ||
    Loader.isModLoaded("MineFactoryReloaded") ||
    Loader.isModLoaded("ProjRed|Transmission")
}
