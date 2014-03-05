package li.cil.oc.common.tileentity

import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network.{Analyzable, Component, Visibility}
import li.cil.oc.common.EventHandler
import li.cil.oc.server.driver.Registry
import li.cil.oc.{api, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class DiskDrive extends Environment with ComponentInventory with Rotatable with Analyzable {
  val node = api.Network.newNode(this, Visibility.None).create()

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) =
    components(0) match {
      case Some(environment) => Array(environment.node)
      case _ => null
    }

  override def canUpdate = false

  override def validate() = {
    super.validate()
    EventHandler.schedule(this)
  }

  // ----------------------------------------------------------------------- //

  override def getInventoryName = Settings.namespace + "container.DiskDrive"

  override def getSizeInventory = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = (slot, Registry.itemDriverFor(stack)) match {
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
