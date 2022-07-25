package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.tileentity
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.IntReferenceHolder
import net.minecraft.util.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class Robot(id: Int, playerInventory: PlayerInventory, robot: tileentity.Robot) extends Player(null, id, playerInventory, robot) {
  val hasScreen: Boolean = robot.components.exists {
    case Some(buffer: api.internal.TextBuffer) => true
    case _ => false
  }
  private val withScreenHeight = 256
  private val noScreenHeight = 108
  val deltaY: Int = if (hasScreen) 0 else withScreenHeight - noScreenHeight

  addSlotToContainer(170 + 0 * slotSize, 232 - deltaY, common.Slot.Tool)
  addSlotToContainer(170 + 1 * slotSize, 232 - deltaY, robot.containerSlotType(1), robot.containerSlotTier(1))
  addSlotToContainer(170 + 2 * slotSize, 232 - deltaY, robot.containerSlotType(2), robot.containerSlotTier(2))
  addSlotToContainer(170 + 3 * slotSize, 232 - deltaY, robot.containerSlotType(3), robot.containerSlotTier(3))

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

  addDataSlot(new IntReferenceHolder {
    override def get(): Int = robot.globalBuffer.toInt / factor

    override def set(value: Int): Unit = robot.globalBuffer = value * factor
  })

  addDataSlot(new IntReferenceHolder {
    override def get(): Int = robot.globalBufferSize.toInt / factor

    override def set(value: Int): Unit = robot.globalBufferSize = value * factor
  })

  class InventorySlot(container: Player, inventory: IInventory, index: Int, x: Int, y: Int, var enabled: Boolean)
    extends StaticComponentSlot(container, inventory, index, x, y, common.Slot.Any, common.Tier.Any) {

    def isValid: Boolean = robot.isInventorySlot(getSlotIndex)

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
