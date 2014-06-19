package li.cil.oc.util

import net.minecraft.entity.item.EntityMinecartContainer
import net.minecraft.inventory.{IInventory, ISidedInventory}
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityChest
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsScala._

object InventoryUtils {
  /**
   * Retrieves an actual inventory implementation for a specified world coordinate.
   * <p/>
   * This performs special handling for (double-)chests and also checks for
   * mine carts with chests.
   */
  def inventoryAt(world: World, x: Int, y: Int, z: Int) = {
    world.getTileEntity(x, y, z) match {
      case chest: TileEntityChest => Option(net.minecraft.init.Blocks.chest.func_149951_m(world, chest.xCoord, chest.yCoord, chest.zCoord))
      case inventory: IInventory => Some(inventory)
      case _ => world.getEntitiesWithinAABB(classOf[EntityMinecartContainer],
        AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1)).
        map(_.asInstanceOf[EntityMinecartContainer]).
        find(!_.isDead)
    }
  }

  /**
   * Inserts a stack into an inventory.
   * <p/>
   * Only tries to insert into the specified slot. This <em>cannot</em> be
   * used to empty a slot. It can only insert stacks into empty slots and
   * merge additional items into an existing stack in the slot.
   * <p/>
   * The passed stack's size will be adjusted to reflect the number of items
   * inserted into the inventory, i.e. if 10 more items could fit into the
   * slot, the stack's size will be 10 smaller than before the call.
   * <p/>
   * This will return <tt>true</tt> if <em>at least</em> one item could be
   * inserted into the slot. It will return <tt>false</tt> if the passed
   * stack did not change.
   * <p/>
   * This takes care of handling special cases such as sided inventories,
   * maximum inventory and item stack sizes.
   * <p/>
   * The number of items inserted can be limited, to avoid unnecessary
   * changes to the inventory the stack may come from, for example.
   */
  def insertIntoInventorySlot(stack: ItemStack, inventory: IInventory, side: ForgeDirection, slot: Int, limit: Int = 64) =
    (stack != null && limit > 0) && {
      val isSideValidForSlot = inventory match {
        case inventory: ISidedInventory => inventory.canInsertItem(slot, stack, side.ordinal)
        case _ => true
      }
      (stack.stackSize > 0 && inventory.isItemValidForSlot(slot, stack) && isSideValidForSlot) && {
        val maxStackSize = math.min(inventory.getInventoryStackLimit, stack.getMaxStackSize)
        val existing = inventory.getStackInSlot(slot)
        val shouldMerge = existing != null && existing.stackSize < maxStackSize &&
          existing.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existing, stack)
        if (shouldMerge) {
          val space = maxStackSize - existing.stackSize
          val amount = math.min(space, math.min(stack.stackSize, limit))
          existing.stackSize += amount
          stack.stackSize -= amount
          inventory.markDirty()
          true
        }
        else (existing == null) && {
          val amount = math.min(maxStackSize, math.min(stack.stackSize, limit))
          inventory.setInventorySlotContents(slot, stack.splitStack(amount))
          true
        }
      }
    }

  /**
   * Extracts a stack from an inventory.
   * <p/>
   * Only tries to extract from the specified slot. This <em>can</em> be used
   * to empty a slot. It will extract items using the specified consumer method
   * which is called with the extracted stack before the stack in the inventory
   * that we extract from is cleared from. This allows placing back excess
   * items with as few inventory updates as possible.
   * <p/>
   * The consumer is the only way to retrieve the actually extracted stack. It
   * is called with a separate stack instance, so it does not have to be copied
   * again.
   * <p/>
   * This will return <tt>true</tt> if <em>at least</em> one item could be
   * extracted from the slot. It will return <tt>false</tt> if the stack in
   * the slot did not change.
   * <p/>
   * This takes care of handling special cases such as sided inventories and
   * maximum stack sizes.
   * <p/>
   * The number of items extracted can be limited, to avoid unnecessary
   * changes to the inventory the stack is extracted from. Note that this could
   * also be achieved by a check in the consumer, but it saves some unnecessary
   * code repetition this way.
   */
  def extractFromInventorySlot(consumer: (ItemStack) => Unit, inventory: IInventory, side: ForgeDirection, slot: Int, limit: Int = 64) = {
    val stack = inventory.getStackInSlot(slot)
    (stack != null && limit > 0) && {
      val isSideValidForSlot = inventory match {
        case inventory: ISidedInventory => inventory.canExtractItem(slot, stack, side.ordinal)
        case _ => true
      }
      (stack.stackSize > 0 && isSideValidForSlot) && {
        val maxStackSize = math.min(inventory.getInventoryStackLimit, stack.getMaxStackSize)
        val amount = math.min(maxStackSize, math.min(stack.stackSize, limit))
        val extracted = stack.splitStack(amount)
        consumer(extracted)
        val success = extracted.stackSize < amount
        stack.stackSize += extracted.stackSize
        if (stack.stackSize == 0) {
          inventory.setInventorySlotContents(slot, null)
        }
        else if (success) {
          inventory.markDirty()
        }
        success
      }
    }
  }

  /**
   * Inserts a stack into an inventory.
   * <p/>
   * This will try to fit the stack in any and as many as necessary slots in
   * the inventory. It will first try to merge the stack in stacks already
   * present in the inventory. After that it will try to fit the stack into
   * empty slots in the inventory.
   * <p/>
   * This uses the <tt>insertIntoInventorySlot</tt> method, and therefore
   * handles special cases such as sided inventories and stack size limits.
   * <p/>
   * This returns <tt>true</tt> if at least one item was inserted. The passed
   * item stack will be adjusted to reflect the number items inserted, by
   * having its size decremented accordingly.
   */
  def insertIntoInventory(stack: ItemStack, inventory: IInventory, side: ForgeDirection, limit: Int = 64) =
    (stack != null && limit > 0) && {
      var success = false
      var remaining = limit

      val shouldTryMerge = !stack.isItemStackDamageable && stack.getMaxStackSize > 1 && inventory.getInventoryStackLimit > 1
      if (shouldTryMerge) {
        for (slot <- 0 until inventory.getSizeInventory) {
          val stackSize = stack.stackSize
          if ((inventory.getStackInSlot(slot) != null) && insertIntoInventorySlot(stack, inventory, side, slot, remaining)) {
            remaining -= stackSize - stack.stackSize
            success = true
          }
        }
      }

      for (slot <- 0 until inventory.getSizeInventory) {
        val stackSize = stack.stackSize
        if ((inventory.getStackInSlot(slot) == null) && insertIntoInventorySlot(stack, inventory, side, slot, remaining)) {
          remaining -= stackSize - stack.stackSize
          success = true
        }
      }

      success
    }

  /**
   * Extracts a slot from an inventory.
   * </p>
   * This will try to extract a stack from any inventory slot. It will iterate
   * all slots until an item can be extracted from a slot.
   * <p/>
   * This uses the <tt>extractFromInventorySlot</tt> method, and therefore
   * handles special cases such as sided inventories and stack size limits.
   * <p/>
   * This returns <tt>true</tt> if at least one item was extracted.
   */
  def extractFromInventory(consumer: (ItemStack) => Unit, inventory: IInventory, side: ForgeDirection, limit: Int = 64) =
    (0 until inventory.getSizeInventory).exists(slot => extractFromInventorySlot(consumer, inventory, side, slot, limit))

  /**
   * Utility method for calling <tt>insertIntoInventory</tt> on an inventory
   * in the world.
   */
  def insertIntoInventoryAt(stack: ItemStack, world: World, x: Int, y: Int, z: Int, side: ForgeDirection, limit: Int = 64): Boolean =
    inventoryAt(world, x, y, z).exists(insertIntoInventory(stack, _, side, limit))

  /**
   * Utility method for calling <tt>extractFromInventory</tt> on an inventory
   * in the world.
   */
  def extractFromInventoryAt(consumer: (ItemStack) => Unit, world: World, x: Int, y: Int, z: Int, side: ForgeDirection, limit: Int = 64) =
    inventoryAt(world, x, y, z).exists(extractFromInventory(consumer, _, side, limit))
}
