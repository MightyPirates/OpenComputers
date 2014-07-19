package li.cil.oc.util.mods

object BundledRedstone {
  def isAvailable = Mods.RedLogic.isAvailable ||
    Mods.MineFactoryReloaded.isAvailable ||
    (Mods.ProjectRedTransmission.isAvailable && ProjectRed.isAPIAvailable)
}
