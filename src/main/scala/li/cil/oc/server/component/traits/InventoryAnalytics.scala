package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.server.component.result
import li.cil.oc.util.DatabaseAccess
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import net.minecraftforge.oredict.OreDictionary

trait InventoryAnalytics extends InventoryAware with NetworkAware {
  @Callback(doc = """function([slot:number]):table -- Get a description of the stack in the specified slot or the selected slot.""")
  def getStackInInternalSlot(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val slot = optSlot(args, 0)
    result(inventory.getStackInSlot(slot))
  }
  else result(Unit, "not enabled in config")

  @Callback(doc = """function(otherSlot:number):boolean -- Get whether the stack in the selected slot is equivalent to the item in the specified slot (have shared OreDictionary IDs).""")
  def isEquivalentTo(context: Context, args: Arguments): Array[AnyRef] = {
    val slot = args.checkSlot(inventory, 0)
    result((stackInSlot(selectedSlot), stackInSlot(slot)) match {
      case (Some(stackA), Some(stackB)) => OreDictionary.getOreIDs(stackA).intersect(OreDictionary.getOreIDs(stackB)).nonEmpty
      case (None, None) => true
      case _ => false
    })
  }

  @Callback(doc = """function(slot:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack description in the specified slot of the database with the specified address.""")
  def storeInternal(context: Context, args: Arguments): Array[AnyRef] = {
    val localSlot = args.checkSlot(inventory, 0)
    val dbAddress = args.checkString(1)
    val localStack = inventory.getStackInSlot(localSlot)
    DatabaseAccess.withDatabase(node, dbAddress, database => {
      val dbSlot = args.checkSlot(database.data, 2)
      val nonEmpty = database.getStackInSlot(dbSlot) != null
      database.setStackInSlot(dbSlot, localStack.copy())
      result(nonEmpty)
    })
  }

  @Callback(doc = """function(slot:number, dbAddress:string, dbSlot:number[, checkNBT:boolean=false]):boolean -- Compare an item in the specified slot with one in the database with the specified address.""")
  def compareToDatabase(context: Context, args: Arguments): Array[AnyRef] = {
    val localSlot = args.checkSlot(inventory, 0)
    val dbAddress = args.checkString(1)
    val localStack = inventory.getStackInSlot(localSlot)
    DatabaseAccess.withDatabase(node, dbAddress, database => {
      val dbSlot = args.checkSlot(database.data, 2)
      val dbStack = database.getStackInSlot(dbSlot)
      result(InventoryUtils.haveSameItemType(localStack, dbStack, args.optBoolean(3, false)))
    })
  }
}
