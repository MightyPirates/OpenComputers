package li.cil.oc.common.item

import li.cil.oc.integration.Mods

class WorldSensorCard(val parent: Delegator) extends traits.Delegate with traits.ItemTier {
  showInItemList = Mods.Galacticraft.isModAvailable
}
