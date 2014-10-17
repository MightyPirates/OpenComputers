package li.cil.oc.integration.util

object Waila {
  def isSavingForTooltip = new Exception().getStackTrace.exists(_.getClassName.startsWith("mcp.mobius.waila"))
}
