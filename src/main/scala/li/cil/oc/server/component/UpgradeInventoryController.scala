package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.api.tileentity.Robot
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import net.minecraftforge.common.util.ForgeDirection

class UpgradeInventoryController(val host: EnvironmentHost with Robot) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("inventory_controller", Visibility.Neighbors).
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():number -- Get the number of slots in the inventory on the specified side of the robot. Back refers to the robot's own inventory.""")
  def getInventorySize(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForInventory(args, 0)
    if (facing == host.facing.getOpposite) result(host.inventorySize)
    else InventoryUtils.inventoryAt(host.world, math.floor(host.xPosition).toInt + facing.offsetX, math.floor(host.yPosition).toInt + facing.offsetY, math.floor(host.zPosition).toInt + facing.offsetZ) match {
      case Some(inventory) => result(inventory.getSizeInventory)
      case _ => result(Unit, "no inventory")
    }
  }

  @Callback(doc = """function():table -- Get a description of the stack in the the inventory on the specified side of the robot. Back refers to the robot's own inventory.""")
  def getStackInSlot(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val facing = checkSideForInventory(args, 0)
    val slot = args.checkInteger(1) - 1
    if (facing == host.facing.getOpposite) {
      if (slot < 0 || slot >= host.inventorySize) result(Unit)
      else result(host.getStackInSlot(slot + 1 + host.containerCount))
    }
    else InventoryUtils.inventoryAt(host.world, math.floor(host.xPosition).toInt + facing.offsetX, math.floor(host.yPosition).toInt + facing.offsetY, math.floor(host.zPosition).toInt + facing.offsetZ) match {
      case Some(inventory) =>
        if (slot < 0 || slot > inventory.getSizeInventory) result(Unit)
        else result(inventory.getStackInSlot(slot))
      case _ => result(Unit, "no inventory")
    }
  }
  else result(Unit, "not enabled in config")

  @Callback(doc = """function(facing:number, slot:number[, count:number]):boolean -- Drops the selected item stack into the specified slot of an inventory.""")
  def dropIntoSlot(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optionalItemCount(2)
    val selectedSlot = host.selectedSlot
    val stack = host.getStackInSlot(selectedSlot)
    if (stack != null && stack.stackSize > 0) {
      InventoryUtils.inventoryAt(host.world, math.floor(host.xPosition).toInt + facing.offsetX, math.floor(host.yPosition).toInt + facing.offsetY, math.floor(host.zPosition).toInt + facing.offsetZ) match {
        case Some(inventory) if inventory.isUseableByPlayer(host.player) =>
          val slot = args.checkSlot(inventory, 1)
          if (!InventoryUtils.insertIntoInventorySlot(stack, inventory, facing.getOpposite, slot, count)) {
            // Cannot drop into that inventory.
            return result(false, "inventory full/invalid slot")
          }
          else if (stack.stackSize == 0) {
            // Dropped whole stack.
            host.setInventorySlotContents(selectedSlot, null)
          }
          else {
            // Dropped partial stack.
            host.markDirty()
          }
        case _ => return result(false, "no inventory")
      }

      context.pause(Settings.get.dropDelay)

      result(true)
    }
    else result(false)
  }

  @Callback(doc = """function(facing:number, slot:number[, count:number]):boolean -- Sucks items from the specified slot of an inventory.""")
  def suckFromSlot(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optionalItemCount(2)

    InventoryUtils.inventoryAt(host.world, math.floor(host.xPosition).toInt + facing.offsetX, math.floor(host.yPosition).toInt + facing.offsetY, math.floor(host.zPosition).toInt + facing.offsetZ) match {
      case Some(inventory) if inventory.isUseableByPlayer(host.player) =>
        val slot = args.checkSlot(inventory, 1)
        if (InventoryUtils.extractFromInventorySlot(host.player.inventory.addItemStackToInventory, inventory, facing.getOpposite, slot, count)) {
          context.pause(Settings.get.suckDelay)
          result(true)
        }
        else result(false)
      case _ => result(false, "no inventory")
    }
  }

  @Callback(doc = """function():boolean -- Swaps the equipped tool with the content of the currently selected inventory slot.""")
  def equip(context: Context, args: Arguments): Array[AnyRef] = {
    if (host.inventorySize > 0) {
      val selectedSlot = host.selectedSlot
      val equipped = host.getStackInSlot(0)
      val selected = host.getStackInSlot(selectedSlot)
      host.setInventorySlotContents(0, selected)
      host.setInventorySlotContents(selectedSlot, equipped)
      result(true)
    }
    else result(false)
  }

  private def checkSideForInventory(args: Arguments, n: Int) = host.toGlobal(args.checkSide(n, ForgeDirection.SOUTH, ForgeDirection.NORTH, ForgeDirection.UP, ForgeDirection.DOWN))

  private def checkSideForAction(args: Arguments, n: Int) = host.toGlobal(args.checkSideForAction(n))
}
