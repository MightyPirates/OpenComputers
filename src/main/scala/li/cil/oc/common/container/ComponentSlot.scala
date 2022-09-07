package li.cil.oc.common.container

import li.cil.oc.api.Driver
import li.cil.oc.api.internal
import li.cil.oc.common
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.container.Slot
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.convert.ImplicitConversionsToScala._

abstract class ComponentSlot(inventory: IInventory, index: Int, x: Int, y: Int) extends Slot(inventory, index, x, y) {
  def agentContainer: Player

  def slot: String

  def tier: Int

  def tierIcon: ResourceLocation

  var changeListener: Option[Slot => Unit] = None

  // ----------------------------------------------------------------------- //

  @OnlyIn(Dist.CLIENT)
  def hasBackground = getBackgroundLocation != null

  @OnlyIn(Dist.CLIENT)
  def getBackgroundLocation: ResourceLocation = null

  @OnlyIn(Dist.CLIENT)
  override def isActive = slot != common.Slot.None && tier != common.Tier.None && super.isActive

  override def mayPlace(stack: ItemStack): Boolean = {
    if (!inventory.canPlaceItem(getSlotIndex, stack)) return false
    if (!isActive) return false
    for (driver <- Driver.itemDrivers) {
      if (driver.worksWith(stack)) {
        val slotOk = (slot == common.Slot.Any || driver.slot(stack) == slot)
        val tierOk = (tier == common.Tier.Any || driver.tier(stack) <= tier)
        if (slotOk && tierOk) return true
      }
    }
    false
  }

  override def onTake(player: PlayerEntity, stack: ItemStack) = {
    for (slot <- agentContainer.slots) slot match {
      case dynamic: ComponentSlot => dynamic.clearIfInvalid(player)
      case _ =>
    }
    super.onTake(player, stack)
  }

  override def set(stack: ItemStack): Unit = {
    super.set(stack)
    inventory match {
      case playerAware: common.tileentity.traits.PlayerInputAware =>
        playerAware.onSetInventorySlotContents(agentContainer.playerInventory.player, getSlotIndex, stack)
      case _ =>
    }
  }

  override def setChanged() {
    super.setChanged()
    for (slot <- agentContainer.slots) slot match {
      case dynamic: ComponentSlot => dynamic.clearIfInvalid(agentContainer.playerInventory.player)
      case _ =>
    }
    changeListener.foreach(_(this))
  }

  protected def clearIfInvalid(player: PlayerEntity) {}
}
