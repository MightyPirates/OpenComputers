package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.World

class MotionSensor(val parent: SimpleDelegator) extends SimpleDelegate {
  override protected def customTextures = Array(
    Some("MotionSensorTop"),
    Some("MotionSensorTop"),
    Some("MotionSensorSide"),
    Some("MotionSensorSide"),
    Some("MotionSensorSide"),
    Some("MotionSensorSide")
  )

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.MotionSensor())
}