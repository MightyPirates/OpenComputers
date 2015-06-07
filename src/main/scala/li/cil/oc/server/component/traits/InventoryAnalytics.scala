package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.server.component.result
import li.cil.oc.util.DatabaseAccess
import li.cil.oc.util.ExtendedArguments._

trait InventoryAnalytics extends InventoryAware with NetworkAware {
  @Callback(doc = """function([slot:number]):table -- Get a description of the stack in the specified slot or the selected slot.""")
  def getStackInInternalSlot(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val slot = optSlot(args, 0)
    result(inventory.getStackInSlot(slot))
  }
  else result(Unit, "not enabled in config")

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

  @Callback(doc = """function(slot:number, dbAddress:string, dbSlot:number):boolean -- Compare an item in the specified slot with one in the database with the specified address.""")
  def compareToDatabase(context: Context, args: Arguments): Array[AnyRef] = {
    val localSlot = args.checkSlot(inventory, 0)
    val dbAddress = args.checkString(1)
    val localStack = inventory.getStackInSlot(localSlot)
    DatabaseAccess.withDatabase(node, dbAddress, database => {
      val dbSlot = args.checkSlot(database.data, 2)
      val dbStack = database.getStackInSlot(dbSlot)
      result(haveSameItemType(localStack, dbStack))
    })
  }
}
