package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.prefab.ItemStackArrayValue
import li.cil.oc.server.component.result
import li.cil.oc.util.{BlockPosition, DatabaseAccess, InventoryUtils}
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.ExtendedArguments._
import net.minecraft.block.Block
import li.cil.oc.util.StackOption
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.oredict.OreDictionary

trait WorldInventoryAnalytics extends WorldAware with SideRestricted with NetworkAware {
  @Callback(doc = """function(side:number):number -- Get the number of slots in the inventory on the specified side of the device.""")
  def getInventorySize(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withInventory(facing, inventory => result(inventory.getSlots))
  }

  @Callback(doc = """function(side:number, slot:number):number -- Get number of items in the specified slot of the inventory on the specified side of the device.""")
  def getSlotStackSize(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withInventory(facing, inventory => result(StackOption(inventory.getStackInSlot(args.checkSlot(inventory, 1))).fold(0)(_.getCount)))
  }

  @Callback(doc = """function(side:number, slot:number):number -- Get the maximum number of items in the specified slot of the inventory on the specified side of the device.""")
  def getSlotMaxStackSize(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withInventory(facing, inventory => result(StackOption(inventory.getStackInSlot(args.checkSlot(inventory, 1))).fold(0)(_.getMaxStackSize)))
  }

  @Callback(doc = """function(side:number, slotA:number, slotB:number[, checkNBT:boolean=false]):boolean -- Get whether the items in the two specified slots of the inventory on the specified side of the device are of the same type.""")
  def compareStacks(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withInventory(facing, inventory => {
      val stackA = inventory.getStackInSlot(args.checkSlot(inventory, 1))
      val stackB = inventory.getStackInSlot(args.checkSlot(inventory, 2))
      result(stackA == stackB || InventoryUtils.haveSameItemType(stackA, stackB, args.optBoolean(3, false)))
    })
  }

  @Callback(doc = """function(side:number, slot:number, dbAddress:string, dbSlot:number[, checkNBT:boolean=false]):boolean -- Compare an item in the specified slot in the inventory on the specified side with one in the database with the specified address.""")
  def compareStackToDatabase(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withInventory(facing, inventory => {
      val slot = args.checkSlot(inventory, 1)
      val dbAddress = args.checkString(2)
      val stack = inventory.getStackInSlot(slot)
      DatabaseAccess.withDatabase(node, dbAddress, database => {
        val dbSlot = args.checkSlot(database.data, 3)
        val dbStack = database.getStackInSlot(dbSlot)
        result(InventoryUtils.haveSameItemType(stack, dbStack, args.optBoolean(4, false)))
      })
    })
  }

  @Callback(doc = """function(side:number, slotA:number, slotB:number):boolean -- Get whether the items in the two specified slots of the inventory on the specified side of the device are equivalent (have shared OreDictionary IDs).""")
  def areStacksEquivalent(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    withInventory(facing, inventory => {
      val stackA = inventory.getStackInSlot(args.checkSlot(inventory, 1))
      val stackB = inventory.getStackInSlot(args.checkSlot(inventory, 2))
      result(stackA == stackB ||
        (!stackA.isEmpty && !stackB.isEmpty &&
          OreDictionary.getOreIDs(stackA).intersect(OreDictionary.getOreIDs(stackB)).nonEmpty))
    })
  }

  @Callback(doc = """function(side:number, slot:number):table -- Get a description of the stack in the inventory on the specified side of the device.""")
  def getStackInSlot(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val facing = checkSideForAction(args, 0)
    withInventory(facing, inventory => result(inventory.getStackInSlot(args.checkSlot(inventory, 1))))
  }
  else result(Unit, "not enabled in config")

  @Callback(doc = """function(side:number):userdata -- Get a description of all stacks in the inventory on the specified side of the device.""")
  def getAllStacks(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val facing = checkSideForAction(args, 0)
    withInventory(facing, inventory => {
        val stacks = new Array[ItemStack](inventory.getSlots)
        for(i <- 0 until inventory.getSlots){
          stacks(i) = inventory.getStackInSlot(i)
        }
        result(new ItemStackArrayValue(stacks))
      })
  }
  else result(Unit, "not enabled in config")

  @Callback(doc = """function(side:number):string -- Get the the name of the inventory on the specified side of the device.""")
  def getInventoryName(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val facing = checkSideForAction(args, 0)
    def blockAt(position: BlockPosition): Option[Block] = position.world match {
      case Some(world) if world.blockExists(position) => world.getBlock(position) match {
        case block: Block => Some(block)
        case _ => None
      }
      case _ => None
    }
    withInventory(facing, inventory => blockAt(position.offset(facing)) match {
      case Some(block) => result(block.getRegistryName)
      case _ => result(Unit, "Unknown")
    })
  }
  else result(Unit, "not enabled in config")

  @Callback(doc = """function(side:number, slot:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack description in the specified slot of the database with the specified address.""")
  def store(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val dbAddress = args.checkString(2)
    def store(stack: ItemStack) = DatabaseAccess.withDatabase(node, dbAddress, database => {
      val dbSlot = args.checkSlot(database.data, 3)
      val nonEmpty = database.getStackInSlot(dbSlot) != ItemStack.EMPTY // zero size stacks
      database.setStackInSlot(dbSlot, stack.copy())
      result(nonEmpty)
    })
    withInventory(facing, inventory => store(inventory.getStackInSlot(args.checkSlot(inventory, 1))))
  }

  private def withInventory(side: EnumFacing, f: IItemHandler => Array[AnyRef]) =
    InventoryUtils.inventoryAt(position.offset(side), side.getOpposite) match {
      case Some(inventory) if mayInteract(position.offset(side), side.getOpposite, inventory) => f(inventory)
      case _ => result(Unit, "no inventory")
    }
}
