package li.cil.oc.common.tileentity

import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network.{Component, Visibility}
import li.cil.oc.server.driver.Registry
import li.cil.oc.{Blocks, api, Settings}
import net.minecraft.item.ItemStack

class DiskDrive extends Environment with ComponentInventory with Rotatable {
  val node = api.Network.newNode(this, Visibility.None).create()

  // ----------------------------------------------------------------------- //

  override def canUpdate = false

  override def validate() = {
    super.validate()
    world.scheduleBlockUpdateFromLoad(x, y, z, Blocks.diskDrive.parent.blockID, 0, 0)
  }

  // ----------------------------------------------------------------------- //

  def getInvName = Settings.namespace + "container.DiskDrive"

  def getSizeInventory = 1

  def isItemValidForSlot(slot: Int, stack: ItemStack) = (slot, Registry.driverFor(stack)) match {
    case (0, Some(driver)) => driver.slot(stack) == Slot.Disk
    case _ => false
  }

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    components(slot) match {
      case Some(environment) => environment.node match {
        case component: Component => component.setVisibility(Visibility.Network)
      }
      case _ =>
    }
  }
}
