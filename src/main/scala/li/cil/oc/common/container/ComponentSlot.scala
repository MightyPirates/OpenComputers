package li.cil.oc.common.container

import li.cil.oc.api
import li.cil.oc.common.InventorySlots.Tier
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.util.Icon
import scala.collection.convert.WrapAsScala._
import cpw.mods.fml.relauncher.{SideOnly, Side}

trait ComponentSlot extends Slot {
  def container: Player

  def slot: api.driver.Slot

  def tier: Int

  def tierIcon: Icon

  @SideOnly(Side.CLIENT)
  override def func_111238_b() = tier != Tier.None && super.func_111238_b()

  override def isItemValid(stack: ItemStack) = inventory.isItemValidForSlot(slotNumber, stack)

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
