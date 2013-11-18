package li.cil.oc.util

import li.cil.oc.common.tileentity.Robot
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.util.ChunkCoordinates
import net.minecraftforge.common.{ForgeDirection, FakePlayer}
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action
import net.minecraftforge.event.{Event, ForgeEventFactory}

class RobotPlayer(val robot: Robot) extends FakePlayer(robot.world, "OpenComputers") {
  capabilities.allowFlying = true
  capabilities.disableDamage = true
  capabilities.isFlying = true
  val robotInventory = new InventoryRobot(this)
  inventory = robotInventory
  yOffset = 1f

  // TODO override def getBoundingBox = super.getBoundingBox

  override def getPlayerCoordinates = new ChunkCoordinates(robot.x, robot.y, robot.z)

  def activateBlockOrUseItem(x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    val event = ForgeEventFactory.onPlayerInteract(this, Action.RIGHT_CLICK_BLOCK, x, y, z, side)
    if (event.isCanceled) {
      return false
    }

    val world = robot.world
    val stack = inventory.getCurrentItem
    val item = if (stack != null) stack.getItem else null
    if (item != null && item.onItemUseFirst(stack, this, world, x, y, z, side, hitX, hitY, hitZ)) {
      if (stack.stackSize <= 0) ForgeEventFactory.onPlayerDestroyItem(this, stack)
      if (stack.stackSize <= 0) inventory.setInventorySlotContents(0, null)
      return true
    }

    val blockId = world.getBlockId(x, y, z)
    val block = Block.blocksList(blockId)
    val shouldActivate = block != null && (!isSneaking || (item == null || item.shouldPassSneakingClickToBlock(world, x, y, z)))
    val activated = shouldActivate && (event.useBlock == Event.Result.DENY ||
      block.onBlockActivated(world, x, y, z, this, side, hitX, hitY, hitZ))

    activated || (stack != null && ({
      val direction = ForgeDirection.getOrientation(side).getOpposite
      val (onX, onY, onZ) = (x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ)
      val result = stack.tryPlaceItemIntoWorld(this, world, onX, onY, onZ, side, hitX, hitY, hitZ)
      if (stack.stackSize <= 0) ForgeEventFactory.onPlayerDestroyItem(this, stack)
      if (stack.stackSize <= 0) inventory.setInventorySlotContents(0, null)
      result
    } || stack.getMaxItemUseDuration <= 0 && {
      val oldSize = stack.stackSize
      val oldDamage = stack.getItemDamage
      val newStack = stack.useItemRightClick(world, this)
      val stackChanged = newStack != stack || (newStack != null && (newStack.stackSize != oldSize || newStack.getItemDamage != oldDamage))
      stackChanged && {
        if (newStack.stackSize <= 0) ForgeEventFactory.onPlayerDestroyItem(this, newStack)
        if (newStack.stackSize > 0) inventory.setInventorySlotContents(0, newStack)
        else inventory.setInventorySlotContents(0, null)
        true
      }
    }))
  }

  def placeBlock(stack: ItemStack, x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    val event = ForgeEventFactory.onPlayerInteract(this, Action.RIGHT_CLICK_BLOCK, x, y, z, side)
    if (event.isCanceled) {
      return false
    }

    event.useBlock == Event.Result.DENY || {
      val world = robot.world
      val direction = ForgeDirection.getOrientation(side).getOpposite
      val (onX, onY, onZ) = (x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ)
      val result = stack.tryPlaceItemIntoWorld(this, world, onX, onY, onZ, side, hitX, hitY, hitZ)
      if (stack.stackSize <= 0) ForgeEventFactory.onPlayerDestroyItem(this, stack)
      result
    }
  }

  def updatePositionAndRotation(pitch: ForgeDirection) {
    val offsetToGetPistonsToBePlacedProperly = pitch.offsetY * 0.83
    posX = robot.x + 0.5
    posY = robot.y + yOffset - offsetToGetPistonsToBePlacedProperly
    posZ = robot.z + 0.5
    rotationPitch = pitch.offsetY * -90
    rotationYaw = robot.yaw match {
      case ForgeDirection.WEST => 90
      case ForgeDirection.NORTH => 180
      case ForgeDirection.EAST => 270
      case _ => 0
    }

    prevPosX = posZ
    prevPosY = posY
    prevPosZ = posZ
    prevRotationPitch = rotationPitch
    cameraPitch = rotationPitch
    prevCameraPitch = rotationPitch
    prevRotationYaw = rotationYaw
    cameraYaw = rotationYaw
    prevCameraYaw = rotationYaw
  }
}
