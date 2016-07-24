package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.server.component.result
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import net.minecraft.inventory.IInventory
import net.minecraft.util.EnumFacing

trait InventoryWorldControlMk2 extends InventoryAware with WorldAware with SideRestricted {
  @Callback(doc = """function(facing:number, slot:number[, count:number[, fromSide:number]]):boolean -- Drops the selected item stack into the specified slot of an inventory.""")
  def dropIntoSlot(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optItemCount(2)
    val fromSide = args.optSideAny(3, facing.getOpposite)
    val stack = inventory.getStackInSlot(selectedSlot)
    if (stack != null && stack.stackSize > 0) {
      withInventory(position.offset(facing), fromSide, inventory => {
        val slot = args.checkSlot(inventory, 1)
        if (!InventoryUtils.insertIntoInventorySlot(stack, inventory, Option(fromSide), slot, count)) {
          // Cannot drop into that inventory.
          return result(false, "inventory full/invalid slot")
        }
        else if (stack.stackSize == 0) {
          // Dropped whole stack.
          this.inventory.setInventorySlotContents(selectedSlot, null)
        }
        else {
          // Dropped partial stack.
          this.inventory.markDirty()
        }

        context.pause(Settings.get.dropDelay)

        result(true)
      })
    }
    else result(false)
  }

  @Callback(doc = """function(facing:number, slot:number[, count:number[, fromSide:number]]):boolean -- Sucks items from the specified slot of an inventory.""")
  def suckFromSlot(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optItemCount(2)
    val fromSide = args.optSideAny(3, facing.getOpposite)
    withInventory(position.offset(facing), fromSide, inventory => {
      val slot = args.checkSlot(inventory, 1)
      if (InventoryUtils.extractFromInventorySlot(InventoryUtils.insertIntoInventory(_, this.inventory, slots = Option(insertionSlots)), inventory, fromSide, slot, count)) {
        context.pause(Settings.get.suckDelay)
        result(true)
      }
      else result(false)
    })
  }

  private def withInventory(blockPos: BlockPosition, fromSide: EnumFacing, f: IInventory => Array[AnyRef]) =
    InventoryUtils.inventoryAt(blockPos) match {
      case Some(inventory) if inventory.isUseableByPlayer(fakePlayer) && mayInteract(blockPos, fromSide) => f(inventory)
      case _ => result(Unit, "no inventory")
    }
}
