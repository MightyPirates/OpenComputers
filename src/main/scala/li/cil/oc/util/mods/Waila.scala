package li.cil.oc.util.mods

object Waila {
  def isSavingForTooltip = new Exception().getStackTrace.exists(_.getClassName.startsWith("mcp.mobius.waila"))
}
