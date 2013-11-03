package li.cil.oc.common.tileentity

import li.cil.oc.util.Persistable
import net.minecraft.entity.item.EntityItem
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import net.minecraft.world.World

trait Inventory extends IInventory with Persistable {
  protected val inventory = Array.fill[Option[ItemStack]](getSizeInventory)(None)

  def getStackInSlot(i: Int) = inventory(i).orNull

  def decrStackSize(slot: Int, amount: Int) = inventory(slot) match {
    case Some(stack) if stack.stackSize <= amount =>
      setInventorySlotContents(slot, null)
      stack
    case Some(stack) =>
      val result = stack.splitStack(amount)
      onInventoryChanged()
      result
    case _ => null
  }

  def setInventorySlotContents(slot: Int, item: ItemStack) = {
    if (inventory(slot).isDefined)
      onItemRemoved(slot, inventory(slot).get)

    inventory(slot) = Option(item)
    if (item != null && item.stackSize > getInventoryStackLimit)
      item.stackSize = getInventoryStackLimit

    if (inventory(slot).isDefined)
      onItemAdded(slot, inventory(slot).get)

    onInventoryChanged()
  }

  def getStackInSlotOnClosing(slot: Int) = null

  def isInvNameLocalized = false

  def openChest() {}

  def closeChest() {}

  def dropContent(world: World, x: Int, y: Int, z: Int) {
    val rng = world.rand
    for (slot <- 0 until getSizeInventory) {
      inventory(slot) match {
        case Some(stack) if stack.stackSize > 0 =>
          setInventorySlotContents(slot, null)
          val (tx, ty, tz) = (0.25 + (rng.nextDouble() * 0.5), 0.25 + (rng.nextDouble() * 0.5), 0.25 + (rng.nextDouble() * 0.5))
          val (vx, vy, vz) = ((rng.nextDouble() - 0.3) * 0.5, (rng.nextDouble() - 0.5) * 0.3, (rng.nextDouble() - 0.5) * 0.3)
          val entity = new EntityItem(world, x + tx, y + ty, z + tz, stack.copy())
          entity.setVelocity(vx, vy, vz)
          entity.delayBeforeCanPickup = 20
          world.spawnEntityInWorld(entity)
        case _ => // Nothing.
      }
    }
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    val inventoryNbt = nbt.getTagList("oc.inventory.list")
    for (i <- 0 until inventoryNbt.tagCount) {
      val slotNbt = inventoryNbt.tagAt(i).asInstanceOf[NBTTagCompound]
      val slot = slotNbt.getByte("slot")
      if (slot >= 0 && slot < inventory.length) {
        val item = ItemStack.loadItemStackFromNBT(slotNbt.getCompoundTag("item"))
        inventory(slot) = Some(item)
      }
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    val inventoryNbt = new NBTTagList()
    inventory.zipWithIndex collect {
      case (Some(stack), slot) => (stack, slot)
    } foreach {
      case (stack, slot) => {
        val slotNbt = new NBTTagCompound()
        slotNbt.setByte("slot", slot.toByte)
        val itemNbt = new NBTTagCompound()
        stack.writeToNBT(itemNbt)
        slotNbt.setCompoundTag("item", itemNbt)
        inventoryNbt.appendTag(slotNbt)
      }
    }
    nbt.setTag("oc.inventory.list", inventoryNbt)
  }

  protected def onItemAdded(slot: Int, item: ItemStack) {}

  protected def onItemRemoved(slot: Int, item: ItemStack) {}
}
