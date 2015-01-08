package li.cil.oc.common.item

import li.cil.oc.integration.Mods

class WorldSensorCard(val parent: Delegator) extends Delegate with ItemTier {
  showInItemList = Mods.Galacticraft.isAvailable
}
