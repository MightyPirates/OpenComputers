package li.cil.oc.common.container

import li.cil.oc.common
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.convert.WrapAsScala._

trait ComponentSlot extends Slot {
  def container: Player

  def slot: String

  def tier: Int

  def tierIcon: ResourceLocation

  var changeListener: Option[Slot => Unit] = None

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canBeHovered = slot != common.Slot.None && tier != common.Tier.None && super.canBeHovered

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
    changeListener.foreach(_(this))
  }

  protected def clearIfInvalid(player: EntityPlayer) {}
}
