package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import net.minecraft.world.World

// TODO Remove in 1.7
class AccessPoint extends Switch with traits.PowerAcceptor {
  override def energyThroughput = Settings.get.accessPointRate

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.AccessPoint()
}
