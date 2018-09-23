package li.cil.oc.util

import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.BlockChest
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.item.EntityMinecartContainer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityChest
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsScala._

object InventoryUtils {
  /**
   * Check if two item stacks are of equal type, ignoring the stack size.
   * <p/>
   * Optionally check for equality in NBT data.
   */
  def haveSameItemType(stackA: ItemStack, stackB: ItemStack, checkNBT: Boolean = false) =
    stackA != null && stackB != null &&
      stackA.getItem == stackB.getItem &&
      (!stackA.getHasSubtypes || stackA.getItemDamage == stackB.getItemDamage) &&
      (!checkNBT || ItemStack.areItemStackTagsEqual(stackA, stackB))

  /**
   * Retrieves an actual inventory implementation for a specified world coordinate.
   * <p/>
   * This performs special handling for (double-)chests and also checks for
   * mine carts with chests.
   */
  def inventoryAt(position: BlockPosition): Option[IInventory] = position.world match {
    case Some(world) if world.blockExists(position) => (world.getBlock(position), world.getTileEntity(position)) match {
      case (block: BlockChest, chest: TileEntityChest) => Option(block.func_149951_m(world, chest.xCoord, chest.yCoord, chest.zCoord))
      case (_, inventory: IInventory) => Some(inventory)
      case _ => world.getEntitiesWithinAABB(classOf[EntityMinecartContainer], position.bounds).
        map(_.asInstanceOf[EntityMinecartContainer]).
        find(!_.isDead)
    }
    case _ => None
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
  def insertIntoInventorySlot(stack: ItemStack, inventory: IInventory, side: Option[ForgeDirection], slot: Int, limit: Int = 64, simulate: Boolean = false) =
    (stack != null && limit > 0) && {
      val isSideValidForSlot = (inventory, side) match {
        case (inventory: ISidedInventory, Some(s)) => inventory.canInsertItem(slot, stack, s.ordinal)
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
          stack.stackSize -= amount
          if (simulate) amount > 0
          else {
            existing.stackSize += amount
            inventory.markDirty()
            true
          }
        }
        else (existing == null) && {
          val amount = math.min(maxStackSize, math.min(stack.stackSize, limit))
          val inserted = stack.splitStack(amount)
          if (simulate) amount > 0
          else {
            inventory.setInventorySlotContents(slot, inserted)
            true
          }
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
  def insertIntoInventory(stack: ItemStack, inventory: IInventory, side: Option[ForgeDirection] = None, limit: Int = 64, simulate: Boolean = false, slots: Option[Iterable[Int]] = None) =
    (stack != null && limit > 0) && {
      var success = false
      var remaining = limit
      val range = slots.getOrElse(inventory match {
        case sided: ISidedInventory => sided.getAccessibleSlotsFromSide(side.getOrElse(ForgeDirection.UNKNOWN).ordinal).toIterable
        case _ => 0 until inventory.getSizeInventory
      })

      if (range.nonEmpty) {
        // This is a special case for inserting with an explicit ordering,
        // such as when inserting into robots, where the range starts at the
        // selected slot. In that case we want to prefer inserting into that
        // slot, if at all possible, over merging.
        if (slots.isDefined) {
          val stackSize = stack.stackSize
          if ((inventory.getStackInSlot(range.head) == null) && insertIntoInventorySlot(stack, inventory, side, range.head, remaining, simulate)) {
            remaining -= stackSize - stack.stackSize
            success = true
          }
        }

        val shouldTryMerge = !stack.isItemStackDamageable && stack.getMaxStackSize > 1 && inventory.getInventoryStackLimit > 1
        if (shouldTryMerge) {
          for (slot <- range) {
            val stackSize = stack.stackSize
            if ((inventory.getStackInSlot(slot) != null) && insertIntoInventorySlot(stack, inventory, side, slot, remaining, simulate)) {
              remaining -= stackSize - stack.stackSize
              success = true
            }
          }
        }

        for (slot <- range) {
          val stackSize = stack.stackSize
          if ((inventory.getStackInSlot(slot) == null) && insertIntoInventorySlot(stack, inventory, side, slot, remaining, simulate)) {
            remaining -= stackSize - stack.stackSize
            success = true
          }
        }
      }

      success
    }

  /**
   * Extracts a slot from an inventory.
   * <p/>
   * This will try to extract a stack from any inventory slot. It will iterate
   * all slots until an item can be extracted from a slot.
   * <p/>
   * This uses the <tt>extractFromInventorySlot</tt> method, and therefore
   * handles special cases such as sided inventories and stack size limits.
   * <p/>
   * This returns <tt>true</tt> if at least one item was extracted.
   */
  def extractAnyFromInventory(consumer: (ItemStack) => Unit, inventory: IInventory, side: ForgeDirection, limit: Int = 64) = {
    val range = inventory match {
      case sided: ISidedInventory => sided.getAccessibleSlotsFromSide(side.ordinal).toIterable
      case _ => 0 until inventory.getSizeInventory
    }
    range.exists(slot => extractFromInventorySlot(consumer, inventory, side, slot, limit))
  }

  /**
    * Extracts an item stack from an inventory.
    * <p/>
    * This will try to remove items of the same type as the specified item stack
    * up to the number of the stack's size for all slots in the specified inventory.
    * If exact is true, the items colated will also match meta data
    * <p/>
    * This uses the <tt>extractFromInventorySlot</tt> method, and therefore
    * handles special cases such as sided inventories and stack size limits.
    */
  def extractFromInventory(stack: ItemStack, inventory: IInventory, side: ForgeDirection, simulate: Boolean = false, exact: Boolean = true) : ItemStack = {
    val range = inventory match {
      case sided: ISidedInventory => sided.getAccessibleSlotsFromSide(side.ordinal).toIterable
      case _ => 0 until inventory.getSizeInventory
    }
    val remaining = stack.copy()
    for (slot <- range if remaining.stackSize > 0) {
      extractFromInventorySlot(stackInInv => {
        if (stackInInv != null && remaining.getItem == stackInInv.getItem && (!exact || haveSameItemType(remaining, stackInInv, checkNBT = true))) {
          val transferred = stackInInv.stackSize min remaining.stackSize
          remaining.stackSize -= transferred
          if (!simulate) {
            stackInInv.stackSize -= transferred
          }
        }
      }, inventory, side, slot, remaining.stackSize)
    }
    remaining
  }

  /**
   * Utility method for calling <tt>insertIntoInventory</tt> on an inventory
   * in the world.
   */
  def insertIntoInventoryAt(stack: ItemStack, position: BlockPosition, side: Option[ForgeDirection] = None, limit: Int = 64, simulate: Boolean = false): Boolean =
    inventoryAt(position).exists(insertIntoInventory(stack, _, side, limit, simulate))

  /**
   * Utility method for calling <tt>extractFromInventory</tt> on an inventory
   * in the world.
   */
  def extractFromInventoryAt(consumer: (ItemStack) => Unit, position: BlockPosition, side: ForgeDirection, limit: Int = 64) =
    inventoryAt(position).exists(extractAnyFromInventory(consumer, _, side, limit))

  /**
   * Transfers some items between two inventories.
   * <p/>
   * This will try to extract up the specified number of items from any inventory,
   * then insert it into the specified sink inventory. If the insertion fails, the
   * items will remain in the source inventory.
   * <p/>
   * This uses the <tt>extractFromInventory</tt> and <tt>insertIntoInventory</tt>
   * methods, and therefore handles special cases such as sided inventories and
   * stack size limits.
   * <p/>
   * This returns <tt>true</tt> if at least one item was transferred.
   */
  def transferBetweenInventories(source: IInventory, sourceSide: ForgeDirection, sink: IInventory, sinkSide: Option[ForgeDirection], limit: Int = 64) =
    extractAnyFromInventory(
      insertIntoInventory(_, sink, sinkSide, limit), source, sourceSide, limit)

  /**
   * Like <tt>transferBetweenInventories</tt> but moving between specific slots.
   */
  def transferBetweenInventoriesSlots(source: IInventory, sourceSide: ForgeDirection, sourceSlot: Int, sink: IInventory, sinkSide: Option[ForgeDirection], sinkSlot: Option[Int], limit: Int = 64) =
    sinkSlot match {
      case Some(explicitSinkSlot) =>
        extractFromInventorySlot(
          insertIntoInventorySlot(_, sink, sinkSide, explicitSinkSlot, limit), source, sourceSide, sourceSlot, limit)
      case _ =>
        extractFromInventorySlot(
          insertIntoInventory(_, sink, sinkSide, limit), source, sourceSide, sourceSlot, limit)
    }

  /**
   * Utility method for calling <tt>transferBetweenInventories</tt> on inventories
   * in the world.
   */
  def transferBetweenInventoriesAt(source: BlockPosition, sourceSide: ForgeDirection, sink: BlockPosition, sinkSide: Option[ForgeDirection], limit: Int = 64) =
    inventoryAt(source).exists(sourceInventory =>
      inventoryAt(sink).exists(sinkInventory =>
        transferBetweenInventories(sourceInventory, sourceSide, sinkInventory, sinkSide, limit)))

  /**
   * Utility method for calling <tt>transferBetweenInventoriesSlots</tt> on inventories
   * in the world.
   */
  def transferBetweenInventoriesSlotsAt(sourcePos: BlockPosition, sourceSide: ForgeDirection, sourceSlot: Int, sinkPos: BlockPosition, sinkSide: Option[ForgeDirection], sinkSlot: Option[Int], limit: Int = 64) =
    inventoryAt(sourcePos).exists(sourceInventory =>
      inventoryAt(sinkPos).exists(sinkInventory =>
        transferBetweenInventoriesSlots(sourceInventory, sourceSide, sourceSlot, sinkInventory, sinkSide, sinkSlot, limit)))

  /**
   * Utility method for dropping contents from a single inventory slot into
   * the world.
   */
  def dropSlot(position: BlockPosition, inventory: IInventory, slot: Int, count: Int, direction: Option[ForgeDirection] = None) = {
    Option(inventory.decrStackSize(slot, count)) match {
      case Some(stack) if stack.stackSize > 0 => spawnStackInWorld(position, stack, direction); true
      case _ => false
    }
  }

  /**
   * Utility method for dumping all inventory contents into the world.
   */
  def dropAllSlots(position: BlockPosition, inventory: IInventory): Unit = {
    for (slot <- 0 until inventory.getSizeInventory) {
      Option(inventory.getStackInSlot(slot)) match {
        case Some(stack) if stack.stackSize > 0 =>
          inventory.setInventorySlotContents(slot, null)
          spawnStackInWorld(position, stack)
        case _ => // Nothing.
      }
    }
  }

  /**
   * Try inserting an item stack into a player inventory. If that fails, drop it into the world.
   */
  def addToPlayerInventory(stack: ItemStack, player: EntityPlayer): Unit = {
    if (stack != null) {
      if (player.inventory.addItemStackToInventory(stack)) {
        player.inventory.markDirty()
        if (player.openContainer != null) {
          player.openContainer.detectAndSendChanges()
        }
      }
      if (stack.stackSize > 0) {
        player.dropPlayerItemWithRandomChoice(stack, false)
      }
    }
  }

  /**
   * Utility method for spawning an item stack in the world.
   */
  def spawnStackInWorld(position: BlockPosition, stack: ItemStack, direction: Option[ForgeDirection] = None, validator: Option[EntityItem => Boolean] = None): EntityItem = position.world match {
    case Some(world) if stack != null && stack.stackSize > 0 =>
      val rng = world.rand
      val (ox, oy, oz) = direction.fold((0, 0, 0))(d => (d.offsetX, d.offsetY, d.offsetZ))
      val (tx, ty, tz) = (
        0.1 * (rng.nextDouble - 0.5) + ox * 0.65,
        0.1 * (rng.nextDouble - 0.5) + oy * 0.75 + (ox + oz) * 0.25,
        0.1 * (rng.nextDouble - 0.5) + oz * 0.65)
      val dropPos = position.offset(0.5 + tx, 0.5 + ty, 0.5 + tz)
      val entity = new EntityItem(world, dropPos.xCoord, dropPos.yCoord, dropPos.zCoord, stack.copy())
      entity.motionX = 0.0125 * (rng.nextDouble - 0.5) + ox * 0.03
      entity.motionY = 0.0125 * (rng.nextDouble - 0.5) + oy * 0.08 + (ox + oz) * 0.03
      entity.motionZ = 0.0125 * (rng.nextDouble - 0.5) + oz * 0.03
      entity.delayBeforeCanPickup = 15
      if (validator.fold(true)(_(entity))) {
        world.spawnEntityInWorld(entity)
        entity
      }
      else null
    case _ => null
  }
}
