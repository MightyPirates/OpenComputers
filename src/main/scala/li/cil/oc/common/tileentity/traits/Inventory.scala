package li.cil.oc.common.tileentity.traits

import li.cil.oc.common.inventory
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.text.ITextComponent

trait Inventory extends TileEntity with inventory.Inventory {
  private lazy val inventory = Array.fill[Option[ItemStack]](getSizeInventory)(None)

  def items = inventory

  // ----------------------------------------------------------------------- //

  override def getDisplayName: ITextComponent = super[Inventory].getDisplayName

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    load(nbt)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    save(nbt)
  }

  // ----------------------------------------------------------------------- //

  override def isUseableByPlayer(player: EntityPlayer) =
    player.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64

  // ----------------------------------------------------------------------- //

  def dropSlot(slot: Int, count: Int = getInventoryStackLimit, direction: Option[EnumFacing] = None) =
    InventoryUtils.dropSlot(BlockPosition(x, y, z, world), this, slot, count, direction)

  def dropAllSlots() =
    InventoryUtils.dropAllSlots(BlockPosition(x, y, z, world), this)

  def spawnStackInWorld(stack: ItemStack, direction: Option[EnumFacing] = None) =
    InventoryUtils.spawnStackInWorld(BlockPosition(x, y, z, world), stack, direction)
}
