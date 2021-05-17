package li.cil.oc.server.component.traits

import li.cil.oc.api
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.item.ItemStack
import net.minecraftforge.items.IItemHandler

trait ItemInventoryControl extends InventoryAware {
  @Callback(doc = "function(slot:number):number -- The size of an item inventory in the specified slot.")
  def getItemInventorySize(context: Context, args: Arguments): Array[AnyRef] = {
    withItemInventory(args.checkSlot(inventory, 0), itemInventory => result(itemInventory.getSlots))
  }

  @Callback(doc = "function(inventorySlot:number, slot:number[, count:number=64]):number -- Drops an item from the selected slot into the specified slot in the item inventory.")
  def dropIntoItemInventory(context: Context, args: Arguments): Array[AnyRef] = {
    withItemInventory(args.checkSlot(inventory, 0), itemInventory => {
      val slot = args.checkSlot(itemInventory, 1)
      val count = args.optItemCount(2)
      result(InventoryUtils.extractFromInventorySlot((is, sim) => InventoryUtils.insertIntoInventorySlot(is, itemInventory, slot, simulate = sim), inventory, null, selectedSlot, count))
    })
  }

  @Callback(doc = "function(inventorySlot:number, slot:number[, count:number=64]):number -- Sucks an item out of the specified slot in the item inventory.")
  def suckFromItemInventory(context: Context, args: Arguments): Array[AnyRef] = {
    withItemInventory(args.checkSlot(inventory, 0), itemInventory => {
      val slot = args.checkSlot(itemInventory, 1)
      val count = args.optItemCount(2)
      result(InventoryUtils.extractFromInventorySlot((is, sim) => InventoryUtils.insertIntoInventory(is, InventoryUtils.asItemHandler(inventory), slots = Option(insertionSlots), simulate = sim), itemInventory, slot, count))
    })
  }

  private def withItemInventory(slot: Int, f: IItemHandler => Array[AnyRef]): Array[AnyRef] = {
    inventory.getStackInSlot(slot) match {
      case stack: ItemStack => api.Driver.itemHandlerFor(stack, fakePlayer) match {
        case inventory: IItemHandler => f(inventory)
        case _ => result(0, "no item inventory")
      }
      case _ => result(0, "no item inventory")
    }
  }
}
