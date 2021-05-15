package li.cil.oc.common.container

import li.cil.oc.common
import li.cil.oc.common.InventorySlots.InventorySlot
import li.cil.oc.common.Tier
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.FakePlayer

import scala.collection.convert.WrapAsScala._

abstract class Player(val playerInventory: InventoryPlayer, val otherInventory: IInventory) extends Container {
  /** Number of player inventory slots to display horizontally. */
  protected val playerInventorySizeX = math.min(9, InventoryPlayer.getHotbarSize)

  /** Subtract four for armor slots. */
  protected val playerInventorySizeY = math.min(4, (playerInventory.getSizeInventory - 4) / playerInventorySizeX)

  /** Render size of slots (width and height). */
  protected val slotSize = 18

  private var lastSync = System.currentTimeMillis()

  override def canInteractWith(player: EntityPlayer) = otherInventory.isUsableByPlayer(player)

  override def slotClick(slot: Int, dragType: Int, clickType: ClickType, player: EntityPlayer): ItemStack = {
    val result = super.slotClick(slot, dragType, clickType, player)
    if (SideTracker.isServer) {
      detectAndSendChanges() // We have to enforce this more than MC does itself
      // because stacks can change their... "character" just by being inserted in
      // certain containers - by being assigned an address.
    }
    result
  }

  override def transferStackInSlot(player: EntityPlayer, index: Int): ItemStack = {
    val slot = Option(inventorySlots.get(index)).orNull
    if (slot != null && slot.getHasStack) {
      tryTransferStackInSlot(slot, slot.inventory == otherInventory)
      if (SideTracker.isServer) {
        detectAndSendChanges()
      }
    }
    ItemStack.EMPTY
  }

  // return true if all items have been moved or no more work to do
  protected def tryMoveAllSlotToSlot(from: Slot, to: Slot): Boolean = {
    if (to == null)
      return false // nowhere to move it

    if (from == null ||
       !from.getHasStack ||
        from.getStack.isEmpty)
      return true // all moved because nothing to move

    if (to.inventory == from.inventory)
      return false // not intended for moving in the same inventory

    // for ghost slots we don't care about stack size
    val fromStack = from.getStack
    val toStack = if (to.getHasStack) to.getStack else ItemStack.EMPTY
    val toStackSize = if (!toStack.isEmpty) toStack.getCount else 0

    val maxStackSize = math.min(fromStack.getMaxStackSize, to.getSlotStackLimit)
    val itemsMoved = math.min(maxStackSize - toStackSize, fromStack.getCount)

    if (!toStack.isEmpty) {
      if (toStackSize < maxStackSize &&
          fromStack.isItemEqual(toStack) &&
          ItemStack.areItemStackTagsEqual(fromStack, toStack) &&
          itemsMoved > 0) {
        toStack.grow(from.decrStackSize(itemsMoved).getCount)
      } else return false
    } else if (to.isItemValid(fromStack)) {
      to.putStack(from.decrStackSize(itemsMoved))
      if (maxStackSize == 0) {
        // Special case: we have an inventory with "phantom/ghost stacks", i.e.
        // zero size stacks, usually used for configuring machinery. In that
        // case we stop early if whatever we're shift clicking is already in a
        // slot of the target inventory. This workaround can be problematic if
        // an inventory has both real and phantom slots, but we don't have
        // something like that, yet, so hey.
        return true
      }
    } else return false

    to.onSlotChanged()
    from.onSlotChanged()
    false
  }

  protected def fillOrder(backFill: Boolean): Seq[Int] = {
    (if (backFill) inventorySlots.indices.reverse else inventorySlots.indices).sortBy(i => inventorySlots(i) match {
      case s: Slot if s.getHasStack => -1
      case s: ComponentSlot => s.tier
      case _ => 99
    })
  }

  protected def tryTransferStackInSlot(from: Slot, intoPlayerInventory: Boolean) {
    for (i <- fillOrder(intoPlayerInventory)) {
      if (inventorySlots.get(i) match { case slot: Slot => tryMoveAllSlotToSlot(from, slot) case _ => false })
        return
    }
  }

  def addSlotToContainer(x: Int, y: Int, slot: String = common.Slot.Any, tier: Int = common.Tier.Any) {
    val index = inventorySlots.size
    addSlotToContainer(new StaticComponentSlot(this, otherInventory, index, x, y, slot, tier))
  }

  def addSlotToContainer(x: Int, y: Int, info: Array[Array[InventorySlot]], containerTierGetter: () => Int) {
    val index = inventorySlots.size
    addSlotToContainer(new DynamicComponentSlot(this, otherInventory, index, x, y, slot => info(slot.containerTierGetter())(slot.getSlotIndex), containerTierGetter))
  }

  def addSlotToContainer(x: Int, y: Int, info: DynamicComponentSlot => InventorySlot) {
    val index = inventorySlots.size
    addSlotToContainer(new DynamicComponentSlot(this, otherInventory, index, x, y, info, () => Tier.One))
  }

  /** Render player inventory at the specified coordinates. */
  protected def addPlayerInventorySlots(left: Int, top: Int) = {
    // Show the inventory proper. Start at plus one to skip hot bar.
    for (slotY <- 1 until playerInventorySizeY) {
      for (slotX <- 0 until playerInventorySizeX) {
        val index = slotX + slotY * playerInventorySizeX
        val x = left + slotX * slotSize
        // Compensate for hot bar offset.
        val y = top + (slotY - 1) * slotSize
        addSlotToContainer(new Slot(playerInventory, index, x, y))
      }
    }

    // Show the quick slot bar below the internal inventory.
    val quickBarSpacing = 4
    for (index <- 0 until playerInventorySizeX) {
      val x = left + index * slotSize
      val y = top + slotSize * (playerInventorySizeY - 1) + quickBarSpacing
      addSlotToContainer(new Slot(playerInventory, index, x, y))
    }
  }

  protected def sendWindowProperty(id: Int, value: Int) {
    listeners.foreach(_.sendWindowProperty(this, id, value))
  }

  override def detectAndSendChanges(): Unit = {
    super.detectAndSendChanges()
    if (SideTracker.isServer) {
      val nbt = new NBTTagCompound()
      detectCustomDataChanges(nbt)
      for (entry <- listeners) entry match {
        case _: FakePlayer => // Nope
        case player: EntityPlayerMP => ServerPacketSender.sendContainerUpdate(this, nbt, player)
        case _ =>
      }
    }
  }

  // Used for custom value synchronization, because shorts simply don't cut it most of the time.
  protected def detectCustomDataChanges(nbt: NBTTagCompound): Unit = {
    val delta = synchronizedData.getDelta
    if (delta != null && !delta.hasNoTags) {
      nbt.setTag("delta", delta)
    }
    else if (System.currentTimeMillis() - lastSync > 250) {
      nbt.setTag("delta", synchronizedData)
      lastSync = Long.MaxValue
    }
  }

  def updateCustomData(nbt: NBTTagCompound): Unit = {
    if (nbt.hasKey("delta")) {
      val delta = nbt.getCompoundTag("delta")
      delta.getKeySet.foreach {
        case key: String => synchronizedData.setTag(key, delta.getTag(key))
      }
    }
  }

  protected class SynchronizedData extends NBTTagCompound {
    private var delta = new NBTTagCompound()

    def getDelta: NBTTagCompound = this.synchronized {
      if (delta.hasNoTags) null
      else {
        val result = delta
        delta = new NBTTagCompound()
        result
      }
    }

    override def setTag(key: String, value: NBTBase): Unit = this.synchronized {
      if (!value.equals(getTag(key))) delta.setTag(key, value)
      super.setTag(key, value)
    }

    override def setByte(key: String, value: Byte): Unit = this.synchronized {
      if (value != getByte(key)) delta.setByte(key, value)
      super.setByte(key, value)
    }

    override def setShort(key: String, value: Short): Unit = this.synchronized {
      if (value != getShort(key)) delta.setShort(key, value)
      super.setShort(key, value)
    }

    override def setInteger(key: String, value: Int): Unit = this.synchronized {
      if (value != getInteger(key)) delta.setInteger(key, value)
      super.setInteger(key, value)
    }

    override def setLong(key: String, value: Long): Unit = this.synchronized {
      if (value != getLong(key)) delta.setLong(key, value)
      super.setLong(key, value)
    }

    override def setFloat(key: String, value: Float): Unit = this.synchronized {
      if (value != getFloat(key)) delta.setFloat(key, value)
      super.setFloat(key, value)
    }

    override def setDouble(key: String, value: Double): Unit = this.synchronized {
      if (value != getDouble(key)) delta.setDouble(key, value)
      super.setDouble(key, value)
    }

    override def setString(key: String, value: String): Unit = this.synchronized {
      if (value != getString(key)) delta.setString(key, value)
      super.setString(key, value)
    }

    override def setByteArray(key: String, value: Array[Byte]): Unit = this.synchronized {
      if (value.deep != getByteArray(key).deep) delta.setByteArray(key, value)
      super.setByteArray(key, value)
    }

    override def setIntArray(key: String, value: Array[Int]): Unit = this.synchronized {
      if (value.deep != getIntArray(key).deep) delta.setIntArray(key, value)
      super.setIntArray(key, value)
    }

    override def setBoolean(key: String, value: Boolean): Unit = this.synchronized {
      if (value != getBoolean(key)) delta.setBoolean(key, value)
      super.setBoolean(key, value)
    }
  }

  protected val synchronizedData = new SynchronizedData()

}
