package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.server.component.result
import li.cil.oc.util.DatabaseAccess
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing

trait WorldInventoryAnalytics extends WorldAware with SideRestricted with NetworkAware {
  @Callback(doc = """function(side:number):number -- Get the number of slots in the inventory on the specified side of the device.""")
  def getInventorySize(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withInventory(facing, inventory => result(inventory.getSizeInventory))
  }

  @Callback(doc = """function(side:number, slot:number):number -- Get number of items in the specified slot of the inventory on the specified side of the device.""")
  def getSlotStackSize(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withInventory(facing, inventory => result(Option(inventory.getStackInSlot(args.checkSlot(inventory, 1))).fold(0)(_.stackSize)))
  }

  @Callback(doc = """function(side:number, slot:number):number -- Get the maximum number of items in the specified slot of the inventory on the specified side of the device.""")
  def getSlotMaxStackSize(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withInventory(facing, inventory => result(Option(inventory.getStackInSlot(args.checkSlot(inventory, 1))).fold(0)(_.getMaxStackSize)))
  }

  @Callback(doc = """function(side:number, slotA:number, slotB:number):boolean -- Get whether the items in the two specified slots of the inventory on the specified side of the device are of the same type.""")
  def compareStacks(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withInventory(facing, inventory => {
      val stackA = inventory.getStackInSlot(args.checkSlot(inventory, 1))
      val stackB = inventory.getStackInSlot(args.checkSlot(inventory, 2))
      result(stackA == stackB ||
        (stackA != null && stackB != null &&
          stackA.getItem == stackB.getItem &&
          (!stackA.getHasSubtypes || stackA.getItemDamage == stackB.getItemDamage)))
    })
  }

  @Callback(doc = """function(side:number, slot:number):table -- Get a description of the stack in the inventory on the specified side of the device.""")
  def getStackInSlot(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val facing = checkSideForAction(args, 0)
    withInventory(facing, inventory => result(inventory.getStackInSlot(args.checkSlot(inventory, 1))))
  }
  else result(null, "not enabled in config")

  @Callback(doc = """function(side:number, slot:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack description in the specified slot of the database with the specified address.""")
  def store(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val dbAddress = args.checkString(2)
    def store(stack: ItemStack) = DatabaseAccess.withDatabase(node, dbAddress, database => {
      val dbSlot = args.checkSlot(database.data, 3)
      val nonEmpty = database.data.getStackInSlot(dbSlot) != null
      database.data.setInventorySlotContents(dbSlot, stack.copy())
      result(nonEmpty)
    })
    withInventory(facing, inventory => store(inventory.getStackInSlot(args.checkSlot(inventory, 1))))
  }

  private def withInventory(side: EnumFacing, f: IInventory => Array[AnyRef]) =
    InventoryUtils.inventoryAt(position.offset(side)) match {
      case Some(inventory) if inventory.isUseableByPlayer(fakePlayer) => f(inventory)
      case _ => result(null, "no inventory")
    }
}
