package li.cil.oc.common.container

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.common.Tier
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon

import scala.collection.convert.WrapAsScala._

trait ComponentSlot extends Slot {
  def container: Player

  def slot: String

  def tier: Int

  def tierIcon: IIcon

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
