package li.cil.oc.common.tileentity

import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import li.cil.oc.server.computer.Drivers
import li.cil.oc.api.ComponentType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import li.cil.oc.common.computer.IComputer

trait ItemComponentProxy extends IInventory {
  protected val inventory = new Array[ItemStack](9)

  protected val itemComponents = Array.fill(inventory.length)(0)

  protected val computer: IComputer

  def world: World

  // ----------------------------------------------------------------------- //
  // IInventory
  // ----------------------------------------------------------------------- //

  def getInventoryStackLimit = 1

  def getInvName() = "oc.container.computer"

  def getSizeInventory = inventory.length

  def getStackInSlot(i: Int) = inventory(i)

  def decrStackSize(slot: Int, amount: Int) = {
    val stack = getStackInSlot(slot)
    if (stack == null)
      null
    else if (stack.stackSize <= amount) {
      setInventorySlotContents(slot, null)
      stack
    }
    else {
      val subStack = stack.splitStack(amount)
      if (stack.stackSize == 0) {
        setInventorySlotContents(slot, null)
      }
      subStack
    }
  }

  def getStackInSlotOnClosing(slot: Int) = null

  def setInventorySlotContents(slot: Int, item: ItemStack) = {
    if (itemComponents(slot) != 0) {
      // Uninstall component previously in that slot.
      computer.remove(itemComponents(slot))
      itemComponents(slot) = 0
    }

    inventory(slot) = item
    if (item != null && item.stackSize > getInventoryStackLimit)
      item.stackSize = getInventoryStackLimit

    if (inventory(slot) != null)
      Drivers.driverFor(inventory(slot)) match {
        case None => // Nothing to do, but avoid match errors.
        case Some(driver) => {
          val component = driver.instance.component(inventory(slot))
          val id = driver.instance.id(component)
          itemComponents(slot) =
            if (computer.add(component, driver)) id
            else 0
        }
      }
  }

  def isItemValidForSlot(slot: Int, item: ItemStack) = (slot, Drivers.driverFor(item)) match {
    case (_, None) => false // Invalid item.
    case (0, Some(driver)) => driver.instance.componentType(item) == ComponentType.PSU
    case (1 | 2 | 3, Some(driver)) => driver.instance.componentType(item) == ComponentType.RAM
    case (4 | 5 | 6, Some(driver)) => driver.instance.componentType(item) == ComponentType.HDD
    case (7 | 8, Some(driver)) => driver.instance.componentType(item) == ComponentType.PCI
    case (_, Some(_)) => false // Invalid slot.
  }

  def isInvNameLocalized = false

  def openChest() {}

  def closeChest() {}
}