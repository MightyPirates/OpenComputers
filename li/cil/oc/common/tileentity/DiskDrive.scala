package li.cil.oc.common.tileentity

import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network.{Component, Visibility}
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.server.driver.Registry
import li.cil.oc.{api, Config}
import net.minecraft.item.ItemStack

class DiskDrive extends Environment with ComponentInventory with Rotatable {
  val node = api.Network.newNode(this, Visibility.None).create()

  // ----------------------------------------------------------------------- //

  override def validate() = {
    super.validate()
    if (isClient) {
      ClientPacketSender.sendRotatableStateRequest(this)
    }
  }

  // ----------------------------------------------------------------------- //

  def getInvName = Config.namespace + "container.DiskDrive"

  def getSizeInventory = 1

  def isItemValidForSlot(slot: Int, item: ItemStack) = (slot, Registry.driverFor(item)) match {
    case (0, Some(driver)) => driver.slot(item) == Slot.Disk
    case _ => false
  }

  override protected def onItemAdded(slot: Int, item: ItemStack) {
    super.onItemAdded(slot, item)
    components(slot) match {
      case Some(environment) => environment.node match {
        case component: Component => component.setVisibility(Visibility.Network)
      }
      case _ =>
    }
  }
}
