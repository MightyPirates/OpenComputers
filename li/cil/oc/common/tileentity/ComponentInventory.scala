package li.cil.oc.common.tileentity

import li.cil.oc.Items
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network.Node
import li.cil.oc.common.component
import li.cil.oc.common.item
import li.cil.oc.server.driver.Registry
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.world.World

trait ComponentInventory extends IInventory with Node {
  protected val inventory = new Array[ItemStack](inventorySize)

  protected val itemComponents = Array.fill[Option[Node]](inventorySize)(None)

  protected val computer: component.Computer

  def world: World

  def inventorySize = 8

  def installedMemory = inventory.foldLeft(0)((sum, stack) => sum + (Registry.driverFor(stack) match {
    case Some(driver) if driver.slot(stack) == Slot.RAM => Items.multi.subItem(stack) match {
      case Some(ram: item.Memory) => ram.kiloBytes * 1024
      case _ => 0
    }
    case _ => 0
  }))

  override def load(nbt: NBTTagCompound) = {
    super.load(nbt)
    val list = nbt.getTagList("list")
    for (i <- 0 until list.tagCount) {
      val slotNbt = list.tagAt(i).asInstanceOf[NBTTagCompound]
      val slot = slotNbt.getByte("slot")
      if (slot >= 0 && slot < inventory.length) {
        inventory(slot) = ItemStack.loadItemStackFromNBT(
          slotNbt.getCompoundTag("item"))
        itemComponents(slot) = Registry.driverFor(inventory(slot)) match {
          case None => None
          case Some(driver) => driver.node(inventory(slot))
        }
      }
    }
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)
    val list = new NBTTagList
    inventory.zipWithIndex filter {
      case (stack, slot) => stack != null
    } foreach {
      case (stack, slot) => {
        val slotNbt = new NBTTagCompound
        slotNbt.setByte("slot", slot.toByte)

        itemComponents(slot) match {
          case None => // Nothing special to save.
          case Some(node) =>
            // We're guaranteed to have a driver for entries.
            node.save(Registry.driverFor(stack).get.nbt(stack))
        }

        val itemNbt = new NBTTagCompound
        stack.writeToNBT(itemNbt)
        slotNbt.setCompoundTag("item", itemNbt)
        list.appendTag(slotNbt)
      }
    }
    nbt.setTag("list", list)
  }

  // ----------------------------------------------------------------------- //
  // NetworkNode
  // ----------------------------------------------------------------------- //

  override protected def onConnect() {
    super.onConnect()
    for (node <- itemComponents.filter(_.isDefined).map(_.get))
      network.foreach(_.connect(this, node))
  }

  override protected def onDisconnect() {
    super.onDisconnect()
    for (node <- itemComponents.filter(_.isDefined).map(_.get))
      node.network.foreach(_.remove(node))
  }

  // ----------------------------------------------------------------------- //
  // IInventory
  // ----------------------------------------------------------------------- //

  def getInventoryStackLimit = 1

  def getInvName = "oc.container.computer"

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
    // Uninstall component previously in that slot.
    if (!world.isRemote) itemComponents(slot) match {
      case None => // Nothing to do.
      case Some(node) =>
        itemComponents(slot) = None
        node.network.foreach(_.remove(node))
    }

    inventory(slot) = item
    if (item != null && item.stackSize > getInventoryStackLimit)
      item.stackSize = getInventoryStackLimit

    if (!world.isRemote) Registry.driverFor(inventory(slot)) match {
      case None => // No driver.
      case Some(driver) =>
        driver.node(inventory(slot)) match {
          case None => // No node.
          case Some(node) =>
            itemComponents(slot) = Some(node)
            network.foreach(_.connect(this, node))
        }
    }

    computer.recomputeMemory()
  }

  def isItemValidForSlot(slot: Int, item: ItemStack) = (slot, Registry.driverFor(item)) match {
    case (_, None) => false // Invalid item.
    case (0, Some(driver)) => driver.slot(item) == Slot.PSU
    case (1 | 2 | 3, Some(driver)) => driver.slot(item) == Slot.PCI
    case (4 | 5, Some(driver)) => driver.slot(item) == Slot.RAM
    case (6 | 7, Some(driver)) => driver.slot(item) == Slot.HDD
    case _ => false // Invalid slot.
  }

  def isInvNameLocalized = false

  def openChest() {}

  def closeChest() {}
}