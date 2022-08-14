package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.tileentity
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.ContainerType
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketBuffer
import net.minecraft.util.IntReferenceHolder
import net.minecraft.util.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

object RobotInfo {
  def hasScreen(robot: tileentity.Robot) = robot.components.exists {
    case Some(buffer: api.internal.TextBuffer) => true
    case _ => false
  }

  def readRobotInfo(buff: PacketBuffer): RobotInfo = {
    val mainInvSize = buff.readVarInt()
    val slot1 = buff.readUtf(32)
    val tier1 = buff.readVarInt()
    val slot2 = buff.readUtf(32)
    val tier2 = buff.readVarInt()
    val slot3 = buff.readUtf(32)
    val tier3 = buff.readVarInt()
    val hasScreen = buff.readBoolean()
    new RobotInfo(mainInvSize, slot1, tier1, slot2, tier2, slot3, tier3, hasScreen)
  }
  
  def writeRobotInfo(buff: PacketBuffer, info: RobotInfo) {
    buff.writeVarInt(info.mainInvSize)
    buff.writeUtf(info.slot1, 32)
    buff.writeVarInt(info.tier1)
    buff.writeUtf(info.slot2, 32)
    buff.writeVarInt(info.tier2)
    buff.writeUtf(info.slot3, 32)
    buff.writeVarInt(info.tier3)
    buff.writeBoolean(info.hasScreen)
  }
}

class RobotInfo(val mainInvSize: Int, val slot1: String, val tier1: Int,
    val slot2: String, val tier2: Int, val slot3: String, val tier3: Int, val hasScreen: Boolean) {

  def this(robot: tileentity.Robot) = this(robot.mainInventory.getContainerSize, robot.containerSlotType(1), robot.containerSlotTier(1),
    robot.containerSlotType(2), robot.containerSlotTier(2), robot.containerSlotType(3), robot.containerSlotTier(3),
    RobotInfo.hasScreen(robot))
}

class Robot(selfType: ContainerType[_ <: Robot], id: Int, playerInventory: PlayerInventory, robot: IInventory, info: RobotInfo)
  extends Player(selfType, id, playerInventory, robot) {

  def this(selfType: ContainerType[_ <: Robot], id: Int, playerInventory: PlayerInventory, robot: tileentity.Robot) =
    this(selfType, id, playerInventory, robot, new RobotInfo(robot))

  private val withScreenHeight = 256
  private val noScreenHeight = 108
  val deltaY: Int = if (info.hasScreen) 0 else withScreenHeight - noScreenHeight

  addSlotToContainer(170 + 0 * slotSize, 232 - deltaY, common.Slot.Tool)
  addSlotToContainer(170 + 1 * slotSize, 232 - deltaY, info.slot1, info.tier1)
  addSlotToContainer(170 + 2 * slotSize, 232 - deltaY, info.slot2, info.tier2)
  addSlotToContainer(170 + 3 * slotSize, 232 - deltaY, info.slot3, info.tier3)

  // Slot.x and Slot.y are final, so have to rebuild when scrolling
  def generateSlotsFor(scroll: Int) {
    for (i <- 0 to 15) {
      val y = 156 + (i - scroll) * slotSize - deltaY
      for (j <- 0 to 3) {
        val x = 170 + j * slotSize
        val slot = new InventorySlot(this, otherInventory, slots.size, x, y, i >= scroll && i < scroll + 4)
        val idx = 4 + j + 4 * i
        if (slots.size() <= idx) addSlot(slot)
        else slots.set(idx, slot)
      }
    }
  }
  generateSlotsFor(0)

  addPlayerInventorySlots(6, 174 - deltaY)

  // This factor is used to make the energy values transferable using
  // MCs 'progress bar' stuff, even though those internally send the
  // values as shorts over the net (for whatever reason).
  private val factor = 100

  val globalBuffer = robot match {
    case te: tileentity.Robot => {
      addDataSlot(new IntReferenceHolder {
        override def get(): Int = te.globalBuffer.toInt / factor

        override def set(value: Int): Unit = te.globalBuffer = value * factor
      })
    }
    case _ => addDataSlot(IntReferenceHolder.standalone)
  }

  val globalBufferSize = robot match {
    case te: tileentity.Robot => {
      addDataSlot(new IntReferenceHolder {
        override def get(): Int = te.globalBufferSize.toInt / factor

        override def set(value: Int): Unit = te.globalBufferSize = value * factor
      })
    }
    case _ => addDataSlot(IntReferenceHolder.standalone)
  }

  class InventorySlot(container: Player, inventory: IInventory, index: Int, x: Int, y: Int, var enabled: Boolean)
    extends StaticComponentSlot(container, inventory, index, x, y, common.Slot.Any, common.Tier.Any) {

    def isValid: Boolean = getSlotIndex >= 4 && getSlotIndex < 4 + info.mainInvSize

    @OnlyIn(Dist.CLIENT) override
    def isActive: Boolean = enabled && isValid && super.isActive

    override def getBackgroundLocation: ResourceLocation =
      if (isValid) super.getBackgroundLocation
      else Textures.Icons.get(common.Tier.None)

    override def getItem: ItemStack = {
      if (isValid) super.getItem
      else ItemStack.EMPTY
    }
  }

}
