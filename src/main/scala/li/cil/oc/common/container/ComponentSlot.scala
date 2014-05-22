package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.common.InventorySlots.Tier
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.{IInventory, Slot}
import net.minecraft.item.ItemStack
import net.minecraft.util.Icon
import scala.collection.convert.WrapAsScala._
import cpw.mods.fml.relauncher.{SideOnly, Side}

trait ComponentSlot extends Slot {
  def container: Player

  def slot: api.driver.Slot

  def tier: Int

  def tierIcon: Icon

  // ----------------------------------------------------------------------- //

  var slotIndex = super.getSlotIndex

  override def getStack = inventory.getStackInSlot(slotIndex)

  override def putStack(stack: ItemStack) {
    inventory.setInventorySlotContents(slotIndex, stack)
    onSlotChanged()
  }

  override def decrStackSize(amount: Int) = inventory.decrStackSize(slotIndex, amount)

  override def isSlotInInventory(inventory: IInventory, slot: Int) = inventory == this.inventory && slot == slotIndex

  override def getSlotIndex = slotIndex

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def func_111238_b() = tier != Tier.None && super.func_111238_b()

  override def isItemValid(stack: ItemStack) = inventory.isItemValidForSlot(getSlotIndex, stack)

  override def onPickupFromSlot(player: EntityPlayer, stack: ItemStack) {
    super.onPickupFromSlot(player, stack)
    for (slot <- container.inventorySlots) slot match {
      case dynamic: ComponentSlot => dynamic.clearIfInvalid(player)
      case _ =>
    }
  }

  override def onSlotChanged() {
    super.onSlotChanged()
    for (slot <- container.inventorySlots) slot match {
      case dynamic: ComponentSlot => dynamic.clearIfInvalid(container.playerInventory.player)
      case _ =>
    }
  }

  protected def clearIfInvalid(player: EntityPlayer) {}
}
