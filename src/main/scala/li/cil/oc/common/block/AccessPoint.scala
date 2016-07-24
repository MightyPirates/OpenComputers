package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.ItemBlacklist
import net.minecraft.world.World

// TODO Remove in 1.7
class AccessPoint extends Switch with traits.PowerAcceptor {
  ItemBlacklist.hide(this)

  override def energyThroughput = Settings.get.accessPointRate

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.AccessPoint()
}
