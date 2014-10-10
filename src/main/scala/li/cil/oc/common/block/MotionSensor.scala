package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.World

class MotionSensor extends SimpleBlock {
  override protected def customTextures = Array(
    Some("MotionSensorTop"),
    Some("MotionSensorTop"),
    Some("MotionSensorSide"),
    Some("MotionSensorSide"),
    Some("MotionSensorSide"),
    Some("MotionSensorSide")
  )

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.MotionSensor()
}
