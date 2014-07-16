package li.cil.oc.common.item

import li.cil.oc.util.mods.Mods

class AbstractBusCard(val parent: Delegator) extends Delegate {
  showInItemList = Mods.StargateTech2.isAvailable
}
