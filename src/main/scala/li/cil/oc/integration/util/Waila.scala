package li.cil.oc.integration.util

import li.cil.oc.integration.Mods

object Waila {
  // This is used to check if certain data actually has to be saved in
  // writeToNBT calls. For some stuff we write lots of data (e.g. computer
  // state), and we want to avoid that when Waila is calling us.
  def isSavingForTooltip = Mods.Waila.isModAvailable && new Exception().getStackTrace.exists(_.getClassName.startsWith("mcp.mobius.waila"))
}
