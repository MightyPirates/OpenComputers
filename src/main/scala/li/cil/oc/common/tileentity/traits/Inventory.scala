package li.cil.oc.common.tileentity.traits

import li.cil.oc.common.inventory
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

trait Inventory extends TileEntity with inventory.Inventory {
  lazy val items = Array.fill[Option[ItemStack]](getSizeInventory)(None)

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    load(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    save(nbt)
  }

  // ----------------------------------------------------------------------- //

  override def isUseableByPlayer(player: EntityPlayer) =
    world.getTileEntity(x, y, z) match {
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

  def spawnStackInWorld(stack: ItemStack, direction: ForgeDirection) = if (world != null) {
    val rng = world.rand
    val (tx, ty, tz) = (
      0.1 * (rng.nextDouble - 0.5) + direction.offsetX * 0.65,
      0.1 * (rng.nextDouble - 0.5) + direction.offsetY * 0.75 + (direction.offsetX + direction.offsetZ) * 0.25,
      0.1 * (rng.nextDouble - 0.5) + direction.offsetZ * 0.65)
    val entity = new EntityItem(world, x + 0.5 + tx, y + 0.5 + ty, z + 0.5 + tz, stack.copy())
    entity.motionX = 0.0125 * (rng.nextDouble - 0.5) + direction.offsetX * 0.03
    entity.motionY = 0.0125 * (rng.nextDouble - 0.5) + direction.offsetY * 0.08 + (direction.offsetX + direction.offsetZ) * 0.03
    entity.motionZ = 0.0125 * (rng.nextDouble - 0.5) + direction.offsetZ * 0.03
    entity.delayBeforeCanPickup = 15
    world.spawnEntityInWorld(entity)
    entity
  }
  else null
}
