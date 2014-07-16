package li.cil.oc.common.item

import li.cil.oc.util.mods.Mods

class IronNugget(val parent: Delegator) extends Delegate {
  showInItemList = !Mods.GregTech.isAvailable
}
