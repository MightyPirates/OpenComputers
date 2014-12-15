package li.cil.oc.server.component.traits

import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ResultWrapper.result

trait InventoryInspectable extends InventoryAware {
  @Callback
  def inventorySize(context: Context, args: Arguments): Array[AnyRef] = result(inventory.getSizeInventory)

  @Callback
  def select(context: Context, args: Arguments): Array[AnyRef] = {
    if (args.count > 0 && args.checkAny(0) != null) {
      val slot = args.checkSlot(inventory, 0)
      if (slot != selectedSlot) {
        selectedSlot = slot
      }
    }
    result(selectedSlot + 1)
  }

  @Callback(direct = true)
  def count(context: Context, args: Arguments): Array[AnyRef] = {
    val slot =
      if (args.count > 0 && args.checkAny(0) != null) args.checkSlot(inventory, 0)
      else selectedSlot
    result(stackInSlot(slot) match {
      case Some(stack) => stack.stackSize
      case _ => 0
    })
  }

  @Callback(direct = true)
  def space(context: Context, args: Arguments): Array[AnyRef] = {
    val slot =
      if (args.count > 0 && args.checkAny(0) != null) args.checkSlot(inventory, 0)
      else selectedSlot
    result(stackInSlot(slot) match {
      case Some(stack) => math.min(inventory.getInventoryStackLimit, stack.getMaxStackSize) - stack.stackSize
      case _ => inventory.getInventoryStackLimit
    })
  }

  @Callback
  def compareTo(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = args.checkSlot(inventory, 0)
    result((stackInSlot(selectedSlot), stackInSlot(slot)) match {
      case (Some(stackA), Some(stackB)) => haveSameItemType(stackA, stackB)
      case (None, None) => true
      case _ => false
    })
  }

  @Callback
  def transferTo(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = args.checkSlot(inventory, 0)
    val count = args.optionalItemCount(1)
    if (slot == selectedSlot || count == 0) {
      result(true)
    }
    else result((stackInSlot(selectedSlot), stackInSlot(slot)) match {
      case (Some(from), Some(to)) =>
        if (haveSameItemType(from, to)) {
          val space = math.min(inventory.getInventoryStackLimit, to.getMaxStackSize) - to.stackSize
          val amount = math.min(count, math.min(space, from.stackSize))
          if (amount > 0) {
            from.stackSize -= amount
            to.stackSize += amount
            assert(from.stackSize >= 0)
            if (from.stackSize == 0) {
              inventory.setInventorySlotContents(selectedSlot, null)
            }
            inventory.markDirty()
            true
          }
          else false
        }
        else if (count >= from.stackSize) {
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
