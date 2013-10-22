package li.cil.oc.common.tileentity

import li.cil.oc.api.driver
import li.cil.oc.api.network.Node
import li.cil.oc.server.driver.Registry
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.world.World

trait ComponentInventory extends Inventory with Node {
  protected val components = Array.fill[Option[Node]](getSizeInventory)(None)

  def world: World

  // ----------------------------------------------------------------------- //

  def installedMemory = inventory.foldLeft(0)((sum, stack) => sum + (stack match {
    case Some(item) => Registry.driverFor(item) match {
      case Some(driver: driver.Memory) => driver.amount(item)
      case _ => 0
    }
    case _ => 0
  }))

  // ----------------------------------------------------------------------- //

  override protected def onConnect() {
    super.onConnect()
    components collect {
      case Some(node) => network.foreach(_.connect(this, node))
    }
  }

  override protected def onDisconnect() {
    super.onDisconnect()
    components collect {
      case Some(node) => node.network.foreach(_.remove(node))
    }
  }

  // ----------------------------------------------------------------------- //

  override abstract def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)

    val list = nbt.getTagList("list")
    for (i <- 0 until list.tagCount) {
      val slotNbt = list.tagAt(i).asInstanceOf[NBTTagCompound]
      val slot = slotNbt.getByte("slot")
      if (slot >= 0 && slot < inventory.length) {
        val item = ItemStack.loadItemStackFromNBT(slotNbt.getCompoundTag("item"))
        inventory(slot) = Some(item)
        components(slot) = Registry.driverFor(item) match {
          case Some(driver) =>
            driver.node(item) match {
              case Some(node) =>
                node.readFromNBT(driver.nbt(item))
                Some(node)
              case _ => None
            }
          case _ => None
        }
      }
    }
  }

  override abstract def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)

    val list = new NBTTagList
    inventory.zipWithIndex collect {
      case (Some(stack), slot) => (stack, slot)
    } foreach {
      case (stack, slot) => {
        val slotNbt = new NBTTagCompound
        slotNbt.setByte("slot", slot.toByte)

        components(slot) match {
          case Some(node) =>
            // We're guaranteed to have a driver for entries.
            node.writeToNBT(Registry.driverFor(stack).get.nbt(stack))
          case _ => // Nothing special to save.
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

  def getInventoryStackLimit = 1

  override protected def onItemAdded(slot: Int, item: ItemStack) = if (!world.isRemote) {
    Registry.driverFor(item) match {
      case None => // No driver.
      case Some(driver) => driver.node(item) match {
        case None => // No node.
        case Some(node) =>
          components(slot) = Some(node)
          node.readFromNBT(driver.nbt(item))
          network.foreach(_.connect(this, node))
      }
    }
  }

  override protected def onItemRemoved(slot: Int, item: ItemStack) = if (!world.isRemote) {
    // Uninstall component previously in that slot.
    components(slot) match {
      case Some(node) =>
        // Note to self: we have to remove the node from the network *before*
        // saving, to allow file systems to close their handles before they
        // are saved (otherwise hard drives would restore all handles after
        // being installed into a different computer, even!)
        components(slot) = None
        node.network.foreach(_.remove(node))
        Registry.driverFor(item).foreach(driver => node.writeToNBT(driver.nbt(item)))
      case _ => // Nothing to do.
    }
  }
}