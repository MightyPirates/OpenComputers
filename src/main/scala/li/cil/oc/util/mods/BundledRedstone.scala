package li.cil.oc.util.mods

object BundledRedstone {
  def isAvailable = Mods.RedLogic.isAvailable ||
    Mods.MineFactoryReloaded.isAvailable ||
    (Mods.ProjectRed.isAvailable && ProjectRed.isAPIAvailable)
}
