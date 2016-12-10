package li.cil.oc.util

import li.cil.oc.OpenComputers
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.inventory.ISidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraftforge.items.CapabilityItemHandler
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.IItemHandlerModifiable
import net.minecraftforge.items.wrapper.InvWrapper
import net.minecraftforge.items.wrapper.SidedInvWrapper

import scala.collection.convert.WrapAsScala._

object InventoryUtils {

  def asItemHandler(inventory: IInventory, side: EnumFacing): IItemHandlerModifiable = inventory match {
    case inv: ISidedInventory if side != null => new SidedInvWrapper(inv, side)
    case _ => new InvWrapper(inventory)
  }

  def asItemHandler(inventory: IInventory): IItemHandlerModifiable = asItemHandler(inventory, null)

  /**
   * Check if two item stacks are of equal type, ignoring the stack size.
   * <p/>
   * Optionally check for equality in NBT data.
   */
  def haveSameItemType(stackA: ItemStack, stackB: ItemStack, checkNBT: Boolean = false): Boolean =
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
  def inventoryAt(position: BlockPosition, side: EnumFacing): Option[IItemHandler] = position.world match {
    case Some(world) if world.blockExists(position) => world.getTileEntity(position) match {
      case tile: TileEntity if tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side) => Option(tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side))
      case tile: IInventory => Option(asItemHandler(tile))
      case _ => world.getEntitiesWithinAABB(classOf[Entity], position.bounds)
        .filter(e => !e.isDead && e.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side))
        .map(_.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side))
        .find(_ != null)
    }
    case _ => None
  }

  def anyInventoryAt(position: BlockPosition): Option[IItemHandler] = {
    for(side <- null :: EnumFacing.VALUES.toList) {
      inventoryAt(position, side) match {
        case inv: Some[IItemHandler] => return inv
        case _ =>
      }
    }
    None
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
  def insertIntoInventorySlot(stack: ItemStack, inventory: IItemHandler, slot: Int, limit: Int = 64, simulate: Boolean = false): Boolean =
    (stack != null && limit > 0) && {
      val amount = math.min(stack.stackSize, limit)
      if (simulate) {
        val toInsert = stack.copy()
        toInsert.stackSize = amount
        inventory.insertItem(slot, toInsert, simulate) match {
          case remaining: ItemStack => remaining.stackSize < stack.stackSize
          case _ => true
        }
      } else {
        val toInsert = stack.splitStack(amount)
        inventory.insertItem(slot, toInsert, simulate) match {
          case remaining: ItemStack =>
            val result = remaining.stackSize < stack.stackSize
            stack.stackSize = remaining.stackSize
            result
          case _ => true
        }
      }
    }

  def insertIntoInventorySlot(stack: ItemStack, inventory: IInventory, side: Option[EnumFacing], slot: Int, limit: Int, simulate: Boolean): Boolean =
    insertIntoInventorySlot(stack, asItemHandler(inventory, side.orNull), slot, limit, simulate)

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
  def extractFromInventorySlot(consumer: (ItemStack) => Unit, inventory: IItemHandler, slot: Int, limit: Int = 64): Boolean = {
    val stack = inventory.getStackInSlot(slot)
    (stack != null && limit > 0) && {
      var amount = math.min(stack.getMaxStackSize, math.min(stack.stackSize, limit))
      inventory.extractItem(slot, amount, true) match {
        case extracted: ItemStack =>
          amount = extracted.stackSize
          consumer(extracted)
          if(extracted.stackSize >= amount) return false
          inventory.extractItem(slot, amount - extracted.stackSize, false) match {
            case realExtracted: ItemStack if realExtracted.stackSize == amount - extracted.stackSize => true
            case _ =>
              OpenComputers.log.warn("Items may have been duplicated during inventory extraction. This means an IItemHandler instance acted differently between simulated and non-simulated extraction. Offender: " + inventory)
              true
          }
        case _ => false
      }
    }
  }

  def extractFromInventorySlot(consumer: (ItemStack) => Unit, inventory: IInventory, side: EnumFacing, slot: Int, limit: Int): Boolean = {
    extractFromInventorySlot(consumer, asItemHandler(inventory, side), slot, limit)
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
  def insertIntoInventory(stack: ItemStack, inventory: IItemHandler, limit: Int = 64, simulate: Boolean = false, slots: Option[Iterable[Int]] = None): Boolean =
    (stack != null && limit > 0) && {
      var success = false
      var remaining = limit
      val range = slots.getOrElse(0 until inventory.getSlots)

      if (range.nonEmpty) {
        // This is a special case for inserting with an explicit ordering,
        // such as when inserting into robots, where the range starts at the
        // selected slot. In that case we want to prefer inserting into that
        // slot, if at all possible, over merging.
        if (slots.isDefined) {
          val stackSize = stack.stackSize
          if (insertIntoInventorySlot(stack, inventory, range.head, remaining, simulate)) {
            remaining -= stackSize - stack.stackSize
            success = true
          }
        }

        for (slot <- range) {
          val stackSize = stack.stackSize
          if (insertIntoInventorySlot(stack, inventory, slot, remaining, simulate)) {
            remaining -= stackSize - stack.stackSize
            success = true
          }
        }
      }

      success
    }

  def insertIntoInventory(stack: ItemStack, inventory: IInventory, side: Option[EnumFacing], limit: Int, simulate: Boolean, slots: Option[Iterable[Int]]): Boolean =
    insertIntoInventory(stack, asItemHandler(inventory, side.orNull), limit, simulate, slots)

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
  def extractAnyFromInventory(consumer: (ItemStack) => Unit, inventory: IItemHandler, limit: Int = 64): Boolean =
    (0 until inventory.getSlots).exists(slot => extractFromInventorySlot(consumer, inventory, slot, limit))

  def extractAnyFromInventory(consumer: (ItemStack) => Unit, inventory: IInventory, side: EnumFacing, limit: Int): Boolean =
    extractAnyFromInventory(consumer, asItemHandler(inventory, side), limit)

  /**
   * Extracts an item stack from an inventory.
   * <p/>
   * This will try to remove items of the same type as the specified item stack
   * up to the number of the stack's size for all slots in the specified inventory.
   * <p/>
   * This uses the <tt>extractFromInventorySlot</tt> method, and therefore
   * handles special cases such as sided inventories and stack size limits.
   */
  def extractFromInventory(stack: ItemStack, inventory: IItemHandler, simulate: Boolean = false): ItemStack = {
    val remaining = stack.copy()
    for (slot <- 0 until inventory.getSlots if remaining.stackSize > 0) {
      extractFromInventorySlot(stack => {
        if (haveSameItemType(remaining, stack, checkNBT = true)) {
          val transferred = stack.stackSize min remaining.stackSize
          remaining.stackSize -= transferred
          if (!simulate) {
            stack.stackSize -= transferred
          }
        }
      }, inventory, slot, limit = remaining.stackSize)
    }
    remaining
  }

  def extractFromInventory(stack: ItemStack, inventory: IInventory, side: EnumFacing, simulate: Boolean): ItemStack =
    extractFromInventory(stack, asItemHandler(inventory, side), simulate)

    /**
   * Utility method for calling <tt>insertIntoInventory</tt> on an inventory
   * in the world.
   */
  def insertIntoInventoryAt(stack: ItemStack, position: BlockPosition, side: Option[EnumFacing] = None, limit: Int = 64, simulate: Boolean = false): Boolean =
    inventoryAt(position, side.orNull).exists(insertIntoInventory(stack, _, limit, simulate))

  /**
   * Utility method for calling <tt>extractFromInventory</tt> on an inventory
   * in the world.
   */
  def extractFromInventoryAt(consumer: (ItemStack) => Unit, position: BlockPosition, side: EnumFacing, limit: Int = 64): Boolean =
    inventoryAt(position, side).exists(extractAnyFromInventory(consumer, _, limit))

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
  def transferBetweenInventories(source: IItemHandler, sink: IItemHandler, limit: Int = 64): Boolean =
    extractAnyFromInventory(
      insertIntoInventory(_, sink, limit), source, limit = limit)

  def transferBetweenInventories(source: IInventory, sourceSide: EnumFacing, sink: IInventory, sinkSide: Option[EnumFacing], limit: Int): Boolean =
    transferBetweenInventories(asItemHandler(source, sourceSide), asItemHandler(sink, sinkSide.orNull), limit)

  /**
   * Like <tt>transferBetweenInventories</tt> but moving between specific slots.
   */
  def transferBetweenInventoriesSlots(source: IItemHandler, sourceSlot: Int, sink: IItemHandler, sinkSlot: Option[Int], limit: Int = 64): Boolean =
    sinkSlot match {
      case Some(explicitSinkSlot) =>
        extractFromInventorySlot(
          insertIntoInventorySlot(_, sink, explicitSinkSlot, limit), source, sourceSlot, limit = limit)
      case _ =>
        extractFromInventorySlot(
          insertIntoInventory(_, sink, limit), source, sourceSlot, limit = limit)
    }

  def transferBetweenInventoriesSlots(source: IInventory, sourceSide: EnumFacing, sourceSlot: Int, sink: IInventory, sinkSide: Option[EnumFacing], sinkSlot: Option[Int], limit: Int): Boolean =
    transferBetweenInventoriesSlots(asItemHandler(source, sourceSide), sourceSlot, asItemHandler(sink, sinkSide.orNull), sinkSlot, limit)

  /**
   * Utility method for calling <tt>transferBetweenInventories</tt> on inventories
   * in the world.
   */
  def transferBetweenInventoriesAt(source: BlockPosition, sourceSide: EnumFacing, sink: BlockPosition, sinkSide: Option[EnumFacing], limit: Int = 64): Boolean =
    inventoryAt(source, sourceSide).exists(sourceInventory =>
      inventoryAt(sink, sinkSide.orNull).exists(sinkInventory =>
        transferBetweenInventories(sourceInventory, sinkInventory, limit)))

  /**
   * Utility method for calling <tt>transferBetweenInventoriesSlots</tt> on inventories
   * in the world.
   */
  def transferBetweenInventoriesSlotsAt(sourcePos: BlockPosition, sourceSide: EnumFacing, sourceSlot: Int, sinkPos: BlockPosition, sinkSide: Option[EnumFacing], sinkSlot: Option[Int], limit: Int = 64): Boolean =
    inventoryAt(sourcePos, sourceSide).exists(sourceInventory =>
      inventoryAt(sinkPos, sinkSide.orNull).exists(sinkInventory =>
        transferBetweenInventoriesSlots(sourceInventory, sourceSlot, sinkInventory, sinkSlot, limit)))

  /**
   * Utility method for dropping contents from a single inventory slot into
   * the world.
   */
  def dropSlot(position: BlockPosition, inventory: IInventory, slot: Int, count: Int, direction: Option[EnumFacing] = None): Boolean = {
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
  def spawnStackInWorld(position: BlockPosition, stack: ItemStack, direction: Option[EnumFacing] = None): EntityItem = position.world match {
    case Some(world) if stack != null && stack.stackSize > 0 =>
      val rng = world.rand
      val (ox, oy, oz) = direction.fold((0, 0, 0))(d => (d.getFrontOffsetX, d.getFrontOffsetY, d.getFrontOffsetZ))
      val (tx, ty, tz) = (
        0.1 * (rng.nextDouble - 0.5) + ox * 0.65,
        0.1 * (rng.nextDouble - 0.5) + oy * 0.75 + (ox + oz) * 0.25,
        0.1 * (rng.nextDouble - 0.5) + oz * 0.65)
      val dropPos = position.offset(0.5 + tx, 0.5 + ty, 0.5 + tz)
      val entity = new EntityItem(world, dropPos.xCoord, dropPos.yCoord, dropPos.zCoord, stack.copy())
      entity.motionX = 0.0125 * (rng.nextDouble - 0.5) + ox * 0.03
      entity.motionY = 0.0125 * (rng.nextDouble - 0.5) + oy * 0.08 + (ox + oz) * 0.03
      entity.motionZ = 0.0125 * (rng.nextDouble - 0.5) + oz * 0.03
      entity.setPickupDelay(15)
      world.spawnEntityInWorld(entity)
      entity
    case _ => null
  }
}
