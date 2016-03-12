package li.cil.oc.common.item

import li.cil.oc.Settings

class TerminalServer(val parent: Delegator) extends traits.Delegate {
  override protected def tooltipData = Seq(Settings.get.terminalsPerServer)
}
