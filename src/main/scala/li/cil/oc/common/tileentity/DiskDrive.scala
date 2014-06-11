package li.cil.oc.common.tileentity

import li.cil.oc.api.Driver
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network.{Analyzable, Component, Visibility}
import li.cil.oc.common.Sound
import li.cil.oc.{api, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class DiskDrive extends traits.Environment with traits.ComponentInventory with traits.Rotatable with Analyzable {
  val node = api.Network.newNode(this, Visibility.None).create()

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) =
    components(0) match {
      case Some(environment) => Array(environment.node)
      case _ => null
    }

  override def canUpdate = false

  // ----------------------------------------------------------------------- //

  override def getInventoryName = Settings.namespace + "container.DiskDrive"

  override def getSizeInventory = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = (slot, Option(Driver.driverFor(stack))) match {
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
    Sound.playDiskInsert(this)
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    Sound.playDiskEject(this)
  }
}
