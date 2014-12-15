package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemBlock

trait InventoryWorldInterop extends InventoryAware with WorldAware with SideRestricted {
  @Callback
  def compare(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    stackInSlot(selectedSlot) match {
      case Some(stack) => Option(stack.getItem) match {
        case Some(item: ItemBlock) =>
          val blockPos = BlockPosition(x, y, z, Option(world)).offset(side)
          val idMatches = item.field_150939_a == world.getBlock(blockPos)
          val subTypeMatches = !item.getHasSubtypes || item.getMetadata(stack.getItemDamage) == world.getBlockMetadata(blockPos)
          return result(idMatches && subTypeMatches)
        case _ =>
      }
      case _ =>
    }
    result(false)
  }

  @Callback
  def drop(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optionalItemCount(1)
    val stack = inventory.getStackInSlot(selectedSlot)
    if (stack != null && stack.stackSize > 0) {
      InventoryUtils.inventoryAt(world, x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ) match {
        case Some(inv) if inv.isUseableByPlayer(fakePlayer) =>
          if (!InventoryUtils.insertIntoInventory(stack, inv, Option(facing.getOpposite), count)) {
            // Cannot drop into that inventory.
            return result(false, "inventory full")
          }
          else if (stack.stackSize == 0) {
            // Dropped whole stack.
            inventory.setInventorySlotContents(selectedSlot, null)
          }
          else {
            // Dropped partial stack.
            inventory.markDirty()
          }
        case _ =>
          // No inventory to drop into, drop into the world.
          fakePlayer.dropPlayerItemWithRandomChoice(inventory.decrStackSize(selectedSlot, count), false)
      }

      context.pause(Settings.get.dropDelay)

      result(true)
    }
    else result(false)
  }

  @Callback
  def suck(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optionalItemCount(1)

    if (InventoryUtils.inventoryAt(world, x + facing.offsetX, y + facing.offsetY, z + facing.offsetZ).exists(inventory => {
      inventory.isUseableByPlayer(fakePlayer) && InventoryUtils.extractFromInventory(InventoryUtils.insertIntoInventory(_, this.inventory, slots = Option(insertionSlots)), inventory, facing.getOpposite, count)
    })) {
      context.pause(Settings.get.suckDelay)
      result(true)
    }
    else {
      for (entity <- entitiesOnSide[EntityItem](facing) if !entity.isDead && entity.delayBeforeCanPickup <= 0) {
        val stack = entity.getEntityItem
        val size = stack.stackSize
        onSuckCollect(entity)
        if (stack.stackSize < size || entity.isDead) {
          context.pause(Settings.get.suckDelay)
          return result(true)
        }
      }
      result(false)
    }
  }

  protected def onSuckCollect(entity: EntityItem): Unit = entity.onCollideWithPlayer(fakePlayer)
}
