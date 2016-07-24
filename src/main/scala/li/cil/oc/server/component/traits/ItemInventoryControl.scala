package li.cil.oc.server.component.traits

import li.cil.oc.api
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

trait ItemInventoryControl extends InventoryAware {
  @Callback(doc = "function(slot:number):number -- The size of an item inventory in the specified slot.")
  def getItemInventorySize(context: Context, args: Arguments): Array[AnyRef] = {
    withItemInventory(args.checkSlot(inventory, 0), itemInventory => result(itemInventory.getSizeInventory))
  }

  @Callback(doc = "function(inventorySlot:number, slot:number[, count:number=64]):number -- The size of an item inventory in the specified slot.")
  def dropIntoItemInventory(context: Context, args: Arguments): Array[AnyRef] = {
    withItemInventory(args.checkSlot(inventory, 0), itemInventory => {
      val count = args.optItemCount(1)
      result(InventoryUtils.extractAnyFromInventory(InventoryUtils.insertIntoInventory(_, itemInventory), inventory, null, count))
    })
  }

  @Callback(doc = "function(inventorySlot:number, slot:number[, count:number=64]):number -- The size of an item inventory in the specified slot.")
  def suckFromItemInventory(context: Context, args: Arguments): Array[AnyRef] = {
    withItemInventory(args.checkSlot(inventory, 0), itemInventory => {
      val count = args.optItemCount(1)
      result(InventoryUtils.extractAnyFromInventory(InventoryUtils.insertIntoInventory(_, inventory, slots = Option(insertionSlots)), itemInventory, null, count))
    })
  }

  private def withItemInventory(slot: Int, f: IInventory => Array[AnyRef]): Array[AnyRef] = {
    inventory.getStackInSlot(slot) match {
      case stack: ItemStack => api.Driver.inventoryFor(stack, fakePlayer) match {
        case inventory: IInventory => f(inventory)
        case _ => result(0, "no item inventory")
      }
      case _ => result(0, "no item inventory")
    }
  }
}
