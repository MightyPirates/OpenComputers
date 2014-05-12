package li.cil.oc.common.tileentity

import li.cil.oc.api.network.Visibility
import li.cil.oc.{Settings, api}
import net.minecraft.item.ItemStack
import li.cil.oc.api.Driver
import li.cil.oc.common.InventorySlots.Tier
import li.cil.oc.common.InventorySlots
import li.cil.oc.util.ItemUtils
import li.cil.oc.api.driver.{Slot, UpgradeContainer}

class RobotAssembler extends traits.Environment with traits.Inventory with traits.Rotatable {
  val node = api.Network.newNode(this, Visibility.None).
    withConnector().
    create()

  var isAssembling = false

  def complexity = items.drop(1).foldLeft(0)((acc, stack) => acc + (Option(api.Driver.driverFor(stack.orNull)) match {
    case Some(driver: UpgradeContainer) => (1 + driver.tier(stack.get)) * 2
    case Some(driver) => 1 + driver.tier(stack.get)
    case _ => 0
  }))

  def maxComplexity = {
    val caseTier = ItemUtils.caseTier(items(0).orNull)
    if (caseTier >= 0) Settings.robotComplexityByTier(caseTier) else 0
  }

  def start() {
    if (complexity < maxComplexity) {

    }
  }

  // ----------------------------------------------------------------------- //

  override def getInvName = Settings.namespace + "container.RobotAssembler"

  override def getSizeInventory = 21

  override def getInventoryStackLimit = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    if (slot == 0) {
      val descriptor = api.Items.get(stack)
      descriptor == api.Items.get("case1") ||
        descriptor == api.Items.get("case2") ||
        descriptor == api.Items.get("case3")
    }
    else {
      val caseTier = ItemUtils.caseTier(items(0).orNull)
      caseTier != Tier.None && {
        val info = InventorySlots.assembler(caseTier)(slot)
        Option(Driver.driverFor(stack)) match {
          case Some(driver) if info.slot != Slot.None && info.tier != Tier.None => driver.slot(stack) == info.slot && driver.tier(stack) <= info.tier
          case _ => false
        }
      }
    }

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    updateComplexity()
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    updateComplexity()
  }

  private def updateComplexity() {

  }
}
