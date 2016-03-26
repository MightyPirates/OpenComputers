package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.World

class MotionSensor extends SimpleBlock {
  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.MotionSensor()
}
