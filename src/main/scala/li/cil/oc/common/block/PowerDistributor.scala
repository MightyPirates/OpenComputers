package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.World

class PowerDistributor extends SimpleBlock {
  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.PowerDistributor()
}

