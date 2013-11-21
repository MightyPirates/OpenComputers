package li.cil.oc.common.tileentity

import li.cil.oc.Config
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.Persistable
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

trait Inventory extends TileEntity with IInventory with Persistable {
  protected val items = Array.fill[Option[ItemStack]](getSizeInventory)(None)

  // ----------------------------------------------------------------------- //

  def getStackInSlot(i: Int) = items(i).orNull

  def decrStackSize(slot: Int, amount: Int) = items(slot) match {
    case Some(stack) if stack.stackSize - amount <= 0 =>
      setInventorySlotContents(slot, null)
      stack
    case Some(stack) =>
      val result = stack.splitStack(amount)
      onInventoryChanged()
      result
    case _ => null
  }

  def setInventorySlotContents(slot: Int, item: ItemStack) = {
    if (items(slot).isDefined) {
      onItemRemoved(slot, items(slot).get)
    }

    items(slot) = Option(item)
    if (item != null && item.stackSize > getInventoryStackLimit) {
      item.stackSize = getInventoryStackLimit
    }

    if (items(slot).isDefined) {
      onItemAdded(slot, items(slot).get)
    }

    onInventoryChanged()
  }

  def getStackInSlotOnClosing(slot: Int) = null

  def openChest() {}

  def closeChest() {}

  def isInvNameLocalized = false

  def isUseableByPlayer(player: EntityPlayer) =
    world.getBlockTileEntity(x, y, z) match {
      case t: TileEntity if t == this => player.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64
      case _ => false
    }

  // ----------------------------------------------------------------------- //

  def dropSlot(slot: Int, count: Int = getInventoryStackLimit, direction: ForgeDirection = ForgeDirection.UNKNOWN) = {
    Option(decrStackSize(slot, count)) match {
      case Some(stack) if stack.stackSize > 0 => spawnStackInWorld(stack, direction); true
      case _ => false
    }
  }

  def dropAllSlots() {
    for (slot <- 0 until getSizeInventory) {
      items(slot) match {
        case Some(stack) if stack.stackSize > 0 =>
          setInventorySlotContents(slot, null)
          spawnStackInWorld(stack, ForgeDirection.UNKNOWN)
        case _ => // Nothing.
      }
    }
  }

  def spawnStackInWorld(stack: ItemStack, direction: ForgeDirection) {
    val rng = world.rand
    val (tx, ty, tz) = (
      0.1 * rng.nextGaussian + direction.offsetX * 0.45,
      0.1 * rng.nextGaussian + 0.1,
      0.1 * rng.nextGaussian + direction.offsetZ * 0.45)
    val entity = new EntityItem(world, x + 0.5 + tx, y + 0.5 + ty, z + 0.5 + tz, stack.copy())
    entity.motionX = 0.0125 * rng.nextGaussian + direction.offsetX * 0.03
    entity.motionY = 0.0125 * rng.nextGaussian + 0.03
    entity.motionZ = 0.0125 * rng.nextGaussian + direction.offsetZ * 0.03
    entity.delayBeforeCanPickup = 15
    world.spawnEntityInWorld(entity)
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)

    nbt.getTagList(Config.namespace + "items").foreach[NBTTagCompound](slotNbt => {
      val slot = slotNbt.getByte("slot")
      if (slot >= 0 && slot < items.length) {
        items(slot) = Some(ItemStack.loadItemStackFromNBT(slotNbt.getCompoundTag("item")))
      }
    })
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)

    nbt.setNewTagList(Config.namespace + "items",
      items.zipWithIndex collect {
        case (Some(stack), slot) => (stack, slot)
      } map {
        case (stack, slot) => {
          val slotNbt = new NBTTagCompound()
          slotNbt.setByte("slot", slot.toByte)
          slotNbt.setNewCompoundTag("item", stack.writeToNBT)
        }
      })
  }

  // ----------------------------------------------------------------------- //

  protected def onItemAdded(slot: Int, item: ItemStack) {}

  protected def onItemRemoved(slot: Int, item: ItemStack) {}
}
