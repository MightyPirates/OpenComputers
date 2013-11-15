package li.cil.oc.common.tileentity

import li.cil.oc.Config
import li.cil.oc.api.driver.Slot
import li.cil.oc.server.component
import li.cil.oc.server.driver.Registry
import net.minecraft.item.ItemStack
import net.minecraftforge.common.ForgeDirection

class Case(isClient: Boolean) extends Computer {
  def this() = this(false)

  // ----------------------------------------------------------------------- //

  val instance = if (isClient) null else new component.Computer(this)

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
  }

  // ----------------------------------------------------------------------- //

  def getInvName = Config.namespace + "container.Case"

  def getSizeInventory = 8

  def isItemValidForSlot(slot: Int, item: ItemStack) = (slot, Registry.driverFor(item)) match {
    case (_, None) => false // Invalid item.
    case (0, Some(driver)) => driver.slot(item) == Slot.Power
    case (1 | 2 | 3, Some(driver)) => driver.slot(item) == Slot.Card
    case (4 | 5, Some(driver)) => driver.slot(item) == Slot.Memory
    case (6 | 7, Some(driver)) => driver.slot(item) == Slot.HardDiskDrive
    case _ => false // Invalid slot.
  }

  // ----------------------------------------------------------------------- //

  def canConnectRedstone(side: ForgeDirection) = isOutputEnabled
}