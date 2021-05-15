package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ResultWrapper.result
import li.cil.oc.util.StackOption._
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.item.ItemTossEvent
import net.minecraftforge.fml.common.eventhandler.Event.Result

import scala.collection.convert.WrapAsScala._

trait InventoryWorldControl extends InventoryAware with WorldAware with SideRestricted {
  @Callback(doc = "function(side:number[, fuzzy:boolean=false]):boolean -- Compare the block on the specified side with the one in the selected slot. Returns true if equal.")
  def compare(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    stackInSlot(selectedSlot) match {
      case SomeStack(stack) => Option(stack.getItem) match {
        case Some(item: ItemBlock) =>
          val blockPos = position.offset(side).toBlockPos
          val state = world.getBlockState(blockPos)
          val idMatches = item.getBlock == state.getBlock
          val subTypeMatches = args.optBoolean(1, false) || !item.getHasSubtypes || item.getMetadata(stack.getItemDamage) == state.getBlock.getMetaFromState(state)
          return result(idMatches && subTypeMatches)
        case _ =>
      }
      case _ =>
    }
    result(false)
  }

  @Callback(doc = "function(side:number[, count:number=64]):boolean -- Drops items from the selected slot towards the specified side.")
  def drop(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optItemCount(1)
    val stack = inventory.getStackInSlot(selectedSlot)
    if (!stack.isEmpty && stack.getCount > 0) {
      val blockPos = position.offset(facing)
      InventoryUtils.inventoryAt(blockPos, facing.getOpposite) match {
        case Some(inv) if mayInteract(blockPos, facing.getOpposite, inv) =>
          if (!InventoryUtils.insertIntoInventory(stack, inv, count)) {
            // Cannot drop into that inventory.
            return result(false, "inventory full")
          }
          else if (stack.getCount == 0) {
            // Dropped whole stack.
            inventory.setInventorySlotContents(selectedSlot, ItemStack.EMPTY)
          }
          else {
            // Dropped partial stack.
            inventory.markDirty()
          }
        case _ =>
          // No inventory to drop into, drop into the world.
          val dropped = inventory.decrStackSize(selectedSlot, count)
          val validator = (item: EntityItem) => {
            val event = new ItemTossEvent(item, fakePlayer)
            val canceled = MinecraftForge.EVENT_BUS.post(event)
            val denied = event.hasResult && event.getResult == Result.DENY
            !canceled && !denied
          }
          if (!dropped.isEmpty) {
            if (InventoryUtils.spawnStackInWorld(position, dropped, Some(facing), Some(validator)) == null)
              fakePlayer.inventory.addItemStackToInventory(dropped)
          }
      }

      context.pause(Settings.get.dropDelay)

      result(true)
    }
    else result(false)
  }

  /**
    * @param facing items to suck from
    * @return the number of items sucked
    */
  def suckFromItems(facing: EnumFacing): Int = {
    for (entity <- suckableItems(facing) if !entity.isDead && !entity.cannotPickup) {
      val stack = entity.getItem
      val size = stack.getCount
      onSuckCollect(entity)
      if (stack.getCount < size)
        return size - stack.getCount
      else if (entity.isDead)
        return size
    }
    0
  }

  @Callback(doc = "function(side:number[, count:number=64]):boolean -- Suck up items from the specified side.")
  def suck(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optItemCount(1)

    val blockPos = position.offset(facing)
    var extracted: Int = InventoryUtils.inventoryAt(blockPos, facing.getOpposite) match {
      case Some(inventory) => mayInteract(blockPos, facing.getOpposite)
        InventoryUtils.extractAnyFromInventory(InventoryUtils.insertIntoInventory(_, InventoryUtils.asItemHandler(this.inventory), slots = Option(insertionSlots)), inventory, count)
      case _ => 0
    }
    if (extracted <= 0) {
      extracted = suckFromItems(facing)
    }
    if (extracted <= 0) {
      result(false)
    } else {
      context.pause(Settings.get.suckDelay)
      result(extracted)
    }
  }

  protected def suckableItems(side: EnumFacing) = entitiesOnSide(classOf[EntityItem], side)

  protected def onSuckCollect(entity: EntityItem): Unit = entity.onCollideWithPlayer(fakePlayer)
}
