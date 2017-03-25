package li.cil.oc.common.container

import li.cil.oc.common
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.convert.WrapAsScala._

abstract class ComponentSlot(inventory: IInventory, index: Int, x: Int, y: Int) extends Slot(inventory, index, x, y) {
  def container: AbstractContainerPlayer

  def slot: String

  def tier: Int

  def tierIcon: ResourceLocation

  var changeListener: Option[Slot => Unit] = None

  // ----------------------------------------------------------------------- //

  def hasBackground = backgroundLocation != null

  @SideOnly(Side.CLIENT)
  override def canBeHovered = slot != common.Slot.None && tier != common.Tier.None && super.canBeHovered

  override def isItemValid(stack: ItemStack) = inventory.isItemValidForSlot(getSlotIndex, stack)

  override def onTake(player: EntityPlayer, stack: ItemStack) = {
    for (slot <- container.inventorySlots) slot match {
      case dynamic: ComponentSlot => dynamic.clearIfInvalid(player)
      case _ =>
    }
    super.onTake(player, stack)
  }

  override def putStack(stack: ItemStack): Unit = {
    super.putStack(stack)
    inventory match {
      case playerAware: common.tileentity.traits.PlayerInputAware =>
        playerAware.onSetInventorySlotContents(container.playerInventory.player, getSlotIndex, stack)
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
