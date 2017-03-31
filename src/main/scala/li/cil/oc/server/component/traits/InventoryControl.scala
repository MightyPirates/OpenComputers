package li.cil.oc.server.component.traits

import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.item.ItemStack

trait InventoryControl extends InventoryAware {
  @Callback(doc = "function():number -- The size of this device's internal inventory.")
  def inventorySize(context: Context, args: Arguments): Array[AnyRef] = result(inventory.getSizeInventory)

  @Callback(doc = "function([slot:number]):number -- Get the currently selected slot; set the selected slot if specified.")
  def select(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = optSlot(args, 0)
    if (slot != selectedSlot) {
      selectedSlot = slot
    }
    result(selectedSlot + 1)
  }

  @Callback(direct = true, doc = "function([slot:number]):number -- Get the number of items in the specified slot, otherwise in the selected slot.")
  def count(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = optSlot(args, 0)
    result(stackInSlot(slot) match {
      case Some(stack) => stack.getCount
      case _ => 0
    })
  }

  @Callback(direct = true, doc = "function([slot:number]):number -- Get the remaining space in the specified slot, otherwise in the selected slot.")
  def space(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = optSlot(args, 0)
    result(stackInSlot(slot) match {
      case Some(stack) => math.min(inventory.getInventoryStackLimit, stack.getMaxStackSize) - stack.getCount
      case _ => inventory.getInventoryStackLimit
    })
  }

  @Callback(doc = "function(otherSlot:number[, checkNBT:boolean=false]):boolean -- Compare the contents of the selected slot to the contents of the specified slot.")
  def compareTo(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = args.checkSlot(inventory, 0)
    result((stackInSlot(selectedSlot), stackInSlot(slot)) match {
      case (Some(stackA), Some(stackB)) => InventoryUtils.haveSameItemType(stackA, stackB, args.optBoolean(1, false))
      case (None, None) => true
      case _ => false
    })
  }

  @Callback(doc = "function(toSlot:number[, amount:number]):boolean -- Move up to the specified amount of items from the selected slot into the specified slot.")
  def transferTo(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = args.checkSlot(inventory, 0)
    val count = args.optItemCount(1)
    if (slot == selectedSlot || count == 0) {
      result(true)
    }
    else result((stackInSlot(selectedSlot), stackInSlot(slot)) match {
      case (Some(from), Some(to)) =>
        if (InventoryUtils.haveSameItemType(from, to, checkNBT = true)) {
          val space = math.min(inventory.getInventoryStackLimit, to.getMaxStackSize) - to.getCount
          val amount = math.min(count, math.min(space, from.getCount))
          if (amount > 0) {
            from.shrink(amount)
            to.grow(amount)
            assert(from.getCount >= 0)
            if (from.getCount == 0) {
              inventory.setInventorySlotContents(selectedSlot, ItemStack.EMPTY)
            }
            inventory.markDirty()
            true
          }
          else false
        }
        else if (count >= from.getCount) {
          inventory.setInventorySlotContents(slot, from)
          inventory.setInventorySlotContents(selectedSlot, to)
          true
        }
        else false
      case (Some(from), None) =>
        inventory.setInventorySlotContents(slot, inventory.decrStackSize(selectedSlot, count))
        true
      case _ => false
    })
  }
}
