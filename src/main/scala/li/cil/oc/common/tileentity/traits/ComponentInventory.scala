package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.api.driver.Item
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.network.Node
import li.cil.oc.common.EventHandler
import li.cil.oc.common.inventory
import li.cil.oc.util.ExtendedInventory._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import scala.collection.mutable

trait ComponentInventory extends Environment with Inventory with inventory.ComponentInventory {
  override def host = this

  // ----------------------------------------------------------------------- //

  // Cache changes to inventory slots on the client side to avoid recreating
  // components when we don't have to and the slots are just cleared by MC
  // temporarily.
  private lazy val pendingRemovalsActual = mutable.ArrayBuffer.fill(getSizeInventory)(None: Option[ItemStack])
  private lazy val pendingAddsActual = mutable.ArrayBuffer.fill(getSizeInventory)(None: Option[ItemStack])
  private var updateScheduled = false
  def pendingRemovals = {
    adjustSize(pendingRemovalsActual)
    pendingRemovalsActual
  }
  def pendingAdds = {
    adjustSize(pendingAddsActual)
    pendingAddsActual
  }

  private def adjustSize[T](buffer: mutable.ArrayBuffer[Option[T]]): Unit = {
    val delta = buffer.length - getSizeInventory
    if (delta > 0) {
      buffer.remove(buffer.length - delta, delta)
    }
    else if (delta < 0) {
      buffer.sizeHint(getSizeInventory)
      for (i <- 0 until -delta) {
        buffer += None
      }
    }
  }

  private def applyInventoryChanges(): Unit = {
    updateScheduled = false
    for (slot <- this.indices) {
      (pendingRemovals(slot), pendingAdds(slot)) match {
        case (Some(removed), Some(added)) =>
          if (!removed.isItemEqual(added) || !ItemStack.areItemStackTagsEqual(removed, added)) {
            super.onItemRemoved(slot, removed)
            super.onItemAdded(slot, added)
          } // else: No change, ignore.
        case (Some(removed), None) =>
          super.onItemRemoved(slot, removed)
        case (None, Some(added)) =>
          super.onItemAdded(slot, added)
        case _ => // No change.
      }

      pendingRemovals(slot) = None
      pendingAdds(slot) = None
    }
  }

  private def scheduleInventoryChange(): Unit = {
    if (!updateScheduled) {
      updateScheduled = true
      EventHandler.scheduleClient(() => applyInventoryChanges())
    }
  }

  override protected def onItemAdded(slot: Int, stack: ItemStack): Unit = {
    if (isServer) super.onItemAdded(slot, stack)
    else {
      pendingAdds(slot) = Option(stack)
      scheduleInventoryChange()
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack): Unit = {
    if (isServer) super.onItemRemoved(slot, stack)
    else if (pendingRemovals(slot).isEmpty) {
      pendingRemovals(slot) = Option(stack)
      scheduleInventoryChange()
    }
  }

  override protected def save(component: ManagedEnvironment, driver: Item, stack: ItemStack): Unit = {
    if (isServer) {
      super.save(component, driver, stack)
    }
  }

  // ----------------------------------------------------------------------- //

  override protected def initialize(): Unit = {
    super.initialize()
    if (isClient) {
      connectComponents()
    }
  }

  override def dispose(): Unit = {
    super.dispose()
    if (isClient) {
      disconnectComponents()
    }
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      connectComponents()
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      disconnectComponents()
    }
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    connectComponents()
    super.writeToNBTForClient(nbt)
    save(nbt)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    load(nbt)
    connectComponents()
  }
}
