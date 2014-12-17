package li.cil.oc.common.tileentity.traits

import li.cil.oc.common.inventory
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.InventoryUtils
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
    player.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64

  // ----------------------------------------------------------------------- //

  def dropSlot(slot: Int, count: Int = getInventoryStackLimit, direction: Option[ForgeDirection] = None) =
    InventoryUtils.dropSlot(BlockPosition(x, y, z, Option(world)), this, slot, count, direction)

  def dropAllSlots() =
    InventoryUtils.dropAllSlots(BlockPosition(x, y, z, Option(world)), this)

  def spawnStackInWorld(stack: ItemStack, direction: Option[ForgeDirection] = None) =
    InventoryUtils.spawnStackInWorld(BlockPosition(x, y, z, Option(world)), stack, direction)
}
