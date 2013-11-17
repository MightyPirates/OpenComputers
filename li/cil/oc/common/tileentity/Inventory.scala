package li.cil.oc.common.tileentity

import li.cil.oc.Config
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.Persistable
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

trait Inventory extends TileEntity with IInventory with Persistable {
  protected val items = Array.fill[Option[ItemStack]](getSizeInventory)(None)

  // ----------------------------------------------------------------------- //

  def getStackInSlot(i: Int) = items(i).orNull

  def decrStackSize(slot: Int, amount: Int) = items(slot) match {
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

  def dropContent(world: World, x: Int, y: Int, z: Int) {
    val rng = world.rand
    for (slot <- 0 until getSizeInventory) {
      items(slot) match {
        case Some(stack) if stack.stackSize > 0 =>
          setInventorySlotContents(slot, null)
          val (tx, ty, tz) = (0.25 + (rng.nextDouble() * 0.5), 0.25 + (rng.nextDouble() * 0.5), 0.25 + (rng.nextDouble() * 0.5))
          val (vx, vy, vz) = ((rng.nextDouble() - 0.3) * 0.5, (rng.nextDouble() - 0.5) * 0.3, (rng.nextDouble() - 0.5) * 0.3)
          val entity = new EntityItem(world, x + tx, y + ty, z + tz, stack.copy())
          entity.setVelocity(vx, vy, vz)
          entity.delayBeforeCanPickup = 10
          world.spawnEntityInWorld(entity)
        case _ => // Nothing.
      }
    }
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
