package li.cil.oc.integration.util

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.ModContainer
import cpw.mods.fml.common.versioning.VersionRange
import li.cil.oc.integration.Mods

object Waila {
  private val oldVersion = VersionRange.createFromVersionSpec("[,1.5.6)")

  // This is used to check if certain data actually has to be saved in
  // writeToNBT calls. For some stuff we write lots of data (e.g. computer
  // state), and we want to avoid that when Waila is calling us.
  def isSavingForTooltip = {
    Loader.instance.getIndexedModList.get(Mods.IDs.Waila) match {
      case mod: ModContainer if oldVersion.containsVersion(mod.getProcessedVersion) =>
        // Old version of Waila where we actually have to check.
        new Exception().getStackTrace.exists(_.getClassName.startsWith("mcp.mobius.waila"))
      case _ =>
        // Waila is not present or new enough so we don't care.
        false
    }
  }
}
