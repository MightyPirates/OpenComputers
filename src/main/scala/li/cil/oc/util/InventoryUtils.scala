package li.cil.oc.util

import net.minecraft.world.World
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.block.Block
import net.minecraft.inventory.{IInventory, ISidedInventory}
import net.minecraft.entity.item.EntityMinecartContainer
import net.minecraft.util.AxisAlignedBB
import scala.collection.convert.WrapAsScala._
import net.minecraftforge.common.ForgeDirection

object InventoryUtils {
  def tryDropIntoInventoryAt(stack: ItemStack, world: World, x: Int, y: Int, z: Int, side: ForgeDirection): Boolean = {
    world.getBlockTileEntity(x, y, z) match {
      case chest: TileEntityChest =>
        val inventory = Block.chest.getInventory(world, chest.xCoord, chest.yCoord, chest.zCoord)
        tryDropIntoInventory(stack, inventory, side)
      case inventory: ISidedInventory =>
        tryDropIntoInventory(stack, inventory, side)
      case inventory: IInventory =>
        tryDropIntoInventory(stack, inventory, side)
      case _ =>
        val mineCarts = world.getEntitiesWithinAABB(classOf[EntityMinecartContainer],
          AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1)).
          map(_.asInstanceOf[EntityMinecartContainer])
        for (inventory <- mineCarts if !inventory.isDead) {
          if (tryDropIntoInventory(stack, inventory, side)) {
            return true
          }
        }
        false
    }
  }

  def tryDropIntoInventory(stack: ItemStack, inventory: IInventory, side: ForgeDirection) = {
    def isSideValidForSlot: (Int) => Boolean = inventory match {
      case inventory: ISidedInventory => (slot) => inventory.canInsertItem(slot, stack, side.ordinal)
      case _ => (slot) => true
    }
    var success = false
    val maxStackSize = math.min(inventory.getInventoryStackLimit, stack.getMaxStackSize)
    val shouldTryMerge = !stack.isItemStackDamageable && stack.getMaxStackSize > 1 && inventory.getInventoryStackLimit > 1
    if (shouldTryMerge) {
      for (slot <- 0 until inventory.getSizeInventory if stack.stackSize > 0 && inventory.isItemValidForSlot(slot, stack) && isSideValidForSlot(slot)) {
        val existing = inventory.getStackInSlot(slot)
        val shouldMerge = existing != null && existing.stackSize < maxStackSize &&
          existing.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existing, stack)
        if (shouldMerge) {
          val space = maxStackSize - existing.stackSize
          val amount = math.min(space, stack.stackSize)
          assert(amount > 0)
          success = true
          existing.stackSize += amount
          stack.stackSize -= amount
        }
      }
    }

    for (slot <- 0 until inventory.getSizeInventory if stack.stackSize > 0 && inventory.getStackInSlot(slot) == null && inventory.isItemValidForSlot(slot, stack) && isSideValidForSlot(slot)) {
      val amount = math.min(maxStackSize, stack.stackSize)
      inventory.setInventorySlotContents(slot, stack.splitStack(amount))
      success = true
    }
    if (success) {
      inventory.onInventoryChanged()
    }
    success
  }
}
