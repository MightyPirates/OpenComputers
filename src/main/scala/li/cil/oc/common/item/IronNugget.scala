package li.cil.oc.common.item

import li.cil.oc.integration.Mods

class IronNugget(val parent: Delegator) extends traits.Delegate {
  showInItemList = !Mods.GregTech.isAvailable
}
