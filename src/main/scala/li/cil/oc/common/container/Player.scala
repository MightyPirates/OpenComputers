package li.cil.oc.common.container

import li.cil.oc.common
import li.cil.oc.common.InventorySlots.InventorySlot
import li.cil.oc.common.Tier
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.SideTracker
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.ServerPlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory._
import net.minecraft.inventory.container.ClickType
import net.minecraft.inventory.container.Container
import net.minecraft.inventory.container.ContainerType
import net.minecraft.inventory.container.IContainerListener
import net.minecraft.inventory.container.Slot
import net.minecraft.item.ItemStack
import net.minecraft.nbt.INBT
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.util.FakePlayer

import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable

abstract class Player(selfType: ContainerType[_ <: Player], id: Int, val playerInventory: PlayerInventory, val otherInventory: IInventory) extends Container(selfType, id) {
  /** Number of player inventory slots to display horizontally. */
  protected val playerInventorySizeX = math.min(9, PlayerInventory.getSelectionSize)

  protected val playerInventorySizeY = math.min(4, playerInventory.items.size / playerInventorySizeX)

  /** Render size of slots (width and height). */
  protected val slotSize = 18

  private var lastSync = System.currentTimeMillis()

  protected val playerListeners = mutable.ArrayBuffer.empty[ServerPlayerEntity]

  override def stillValid(player: PlayerEntity) = otherInventory.stillValid(player)

  override def clicked(slot: Int, dragType: Int, clickType: ClickType, player: PlayerEntity): ItemStack = {
    val result = super.clicked(slot, dragType, clickType, player)
    if (SideTracker.isServer) {
      broadcastChanges() // We have to enforce this more than MC does itself
      // because stacks can change their... "character" just by being inserted in
      // certain containers - by being assigned an address.
    }
    result
  }

  override def quickMoveStack(player: PlayerEntity, index: Int): ItemStack = {
    val slot = Option(slots.get(index)).orNull
    if (slot != null && slot.hasItem) {
      tryTransferStackInSlot(slot, slot.container == otherInventory)
      if (SideTracker.isServer) {
        broadcastChanges()
      }
    }
    ItemStack.EMPTY
  }

  // return true if all items have been moved or no more work to do
  protected def tryMoveAllSlotToSlot(from: Slot, to: Slot): Boolean = {
    if (to == null)
      return false // nowhere to move it

    if (from == null ||
       !from.hasItem ||
        from.getItem.isEmpty)
      return true // all moved because nothing to move

    if (to.container == from.container)
      return false // not intended for moving in the same inventory

    // for ghost slots we don't care about stack size
    val fromStack = from.getItem
    val toStack = if (to.hasItem) to.getItem else ItemStack.EMPTY
    val toStackSize = if (!toStack.isEmpty) toStack.getCount else 0

    val maxStackSize = math.min(fromStack.getMaxStackSize, to.getMaxStackSize)
    val itemsMoved = math.min(maxStackSize - toStackSize, fromStack.getCount)

    if (!toStack.isEmpty) {
      if (toStackSize < maxStackSize &&
          fromStack.sameItem(toStack) &&
          ItemStack.tagMatches(fromStack, toStack) &&
          itemsMoved > 0) {
        toStack.grow(from.remove(itemsMoved).getCount)
      } else return false
    } else if (to.mayPlace(fromStack)) {
      to.set(from.remove(itemsMoved))
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

    to.setChanged()
    from.setChanged()
    false
  }

  protected def fillOrder(backFill: Boolean): Seq[Int] = {
    (if (backFill) slots.indices.reverse else slots.indices).sortBy(i => slots(i) match {
      case s: Slot if s.hasItem => -1
      case s: ComponentSlot => s.tier
      case _ => 99
    })
  }

  protected def tryTransferStackInSlot(from: Slot, intoPlayerInventory: Boolean) {
    for (i <- fillOrder(intoPlayerInventory)) {
      if (slots.get(i) match { case slot: Slot => tryMoveAllSlotToSlot(from, slot) case _ => false })
        return
    }
  }

  def addSlotToContainer(x: Int, y: Int, slot: String = common.Slot.Any, tier: Int = common.Tier.Any) {
    val index = slots.size
    addSlot(new StaticComponentSlot(this, otherInventory, index, x, y, slot, tier))
  }

  def addSlotToContainer(x: Int, y: Int, info: Array[Array[InventorySlot]], containerTierGetter: () => Int) {
    val index = slots.size
    addSlot(new DynamicComponentSlot(this, otherInventory, index, x, y, slot => info(slot.containerTierGetter())(slot.getSlotIndex), containerTierGetter))
  }

  def addSlotToContainer(x: Int, y: Int, info: DynamicComponentSlot => InventorySlot) {
    val index = slots.size
    addSlot(new DynamicComponentSlot(this, otherInventory, index, x, y, info, () => Tier.One))
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
        addSlot(new Slot(playerInventory, index, x, y))
      }
    }

    // Show the quick slot bar below the internal inventory.
    val quickBarSpacing = 4
    for (index <- 0 until playerInventorySizeX) {
      val x = left + index * slotSize
      val y = top + slotSize * (playerInventorySizeY - 1) + quickBarSpacing
      addSlot(new Slot(playerInventory, index, x, y))
    }
  }

  override def addSlotListener(listener: IContainerListener): Unit = {
    listener match {
      case _: FakePlayer => // Nope
      case player: ServerPlayerEntity => playerListeners += player
      case _ =>
    }
    super.addSlotListener(listener)
  }

  @OnlyIn(Dist.CLIENT)
  override def removeSlotListener(listener: IContainerListener): Unit = {
    if (listener.isInstanceOf[ServerPlayerEntity]) playerListeners -= listener.asInstanceOf[ServerPlayerEntity]
    super.removeSlotListener(listener)
  }

  override def broadcastChanges(): Unit = {
    super.broadcastChanges()
    if (SideTracker.isServer) {
      val nbt = new CompoundNBT()
      detectCustomDataChanges(nbt)
      for (player <- playerListeners) ServerPacketSender.sendContainerUpdate(this, nbt, player)
    }
  }

  // Used for custom value synchronization, because shorts simply don't cut it most of the time.
  protected def detectCustomDataChanges(nbt: CompoundNBT): Unit = {
    val delta = synchronizedData.getDelta
    if (delta != null && !delta.isEmpty) {
      nbt.put("delta", delta)
    }
    else if (System.currentTimeMillis() - lastSync > 250) {
      nbt.put("delta", synchronizedData)
      lastSync = Long.MaxValue
    }
  }

  def updateCustomData(nbt: CompoundNBT): Unit = {
    if (nbt.contains("delta")) {
      val delta = nbt.getCompound("delta")
      delta.getAllKeys.foreach {
        case key: String => synchronizedData.put(key, delta.get(key))
      }
    }
  }

  protected class SynchronizedData extends CompoundNBT {
    private var delta = new CompoundNBT()

    def getDelta: CompoundNBT = this.synchronized {
      if (delta.isEmpty) null
      else {
        val result = delta
        delta = new CompoundNBT()
        result
      }
    }

    override def put(key: String, value: INBT): INBT = this.synchronized {
      if (!value.equals(get(key))) delta.put(key, value)
      super.put(key, value)
    }

    override def putByte(key: String, value: Byte): Unit = this.synchronized {
      if (value != getByte(key)) delta.putByte(key, value)
      super.putByte(key, value)
    }

    override def putShort(key: String, value: Short): Unit = this.synchronized {
      if (value != getShort(key)) delta.putShort(key, value)
      super.putShort(key, value)
    }

    override def putInt(key: String, value: Int): Unit = this.synchronized {
      if (value != getInt(key)) delta.putInt(key, value)
      super.putInt(key, value)
    }

    override def putLong(key: String, value: Long): Unit = this.synchronized {
      if (value != getLong(key)) delta.putLong(key, value)
      super.putLong(key, value)
    }

    override def putFloat(key: String, value: Float): Unit = this.synchronized {
      if (value != getFloat(key)) delta.putFloat(key, value)
      super.putFloat(key, value)
    }

    override def putDouble(key: String, value: Double): Unit = this.synchronized {
      if (value != getDouble(key)) delta.putDouble(key, value)
      super.putDouble(key, value)
    }

    override def putString(key: String, value: String): Unit = this.synchronized {
      if (value != getString(key)) delta.putString(key, value)
      super.putString(key, value)
    }

    override def putByteArray(key: String, value: Array[Byte]): Unit = this.synchronized {
      if (value.deep != getByteArray(key).deep) delta.putByteArray(key, value)
      super.putByteArray(key, value)
    }

    override def putIntArray(key: String, value: Array[Int]): Unit = this.synchronized {
      if (value.deep != getIntArray(key).deep) delta.putIntArray(key, value)
      super.putIntArray(key, value)
    }

    override def putBoolean(key: String, value: Boolean): Unit = this.synchronized {
      if (value != getBoolean(key)) delta.putBoolean(key, value)
      super.putBoolean(key, value)
    }
  }

  protected val synchronizedData = new SynchronizedData()

}
