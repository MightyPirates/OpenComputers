package li.cil.oc.util

import li.cil.oc.common.tileentity.Robot
import net.minecraft.util.ChunkCoordinates
import net.minecraftforge.common.FakePlayer

class RobotPlayer(val robot: Robot) extends FakePlayer(robot.world, "OpenComputers") {
  capabilities.allowFlying = true
  capabilities.disableDamage = true
  capabilities.isFlying = true
  inventory = new InventoryRobot(this)

  // TODO override def getBoundingBox = super.getBoundingBox

  override def getPlayerCoordinates = new ChunkCoordinates(robot.x, robot.y, robot.z)
}
