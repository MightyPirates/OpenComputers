package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

class MotionSensor extends SimpleBlock {
  override def newBlockEntity(world: IBlockReader) = new tileentity.MotionSensor()
}
