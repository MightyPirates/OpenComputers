package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.internal.Adapter
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.world.WorldServer
import net.minecraftforge.common.util.FakePlayerFactory
import net.minecraftforge.common.util.ForgeDirection

class UpgradeInventoryControllerInAdapter(val host: EnvironmentHost with Adapter) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("inventory_controller", Visibility.Network).
    create()

  private val fakePlayer = FakePlayerFactory.get(host.world.asInstanceOf[WorldServer], Settings.get.fakePlayerProfile)

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(side:number):number -- Get the number of slots in the inventory on the specified side of the adapter.""")
  def getInventorySize(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
    InventoryUtils.inventoryAt(BlockPosition(host).offset(facing)) match {
      case Some(inventory) if checkPermission(inventory) => result(inventory.getSizeInventory)
      case _ => result(Unit, "no inventory")
    }
  }

  @Callback(doc = """function(side:number, slot:number):number -- Get number of items in the specified slot of the inventory on the specified side of the adapter.""")
  def getSlotStackSize(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
    InventoryUtils.inventoryAt(BlockPosition(host).offset(facing)) match {
      case Some(inventory) if checkPermission(inventory) =>
        result(Option(inventory.getStackInSlot(args.checkSlot(inventory, 1))).fold(0)(_.stackSize))
      case _ => result(Unit, "no inventory")
    }
  }

  @Callback(doc = """function(side:number, slot:number):number -- Get the maximum number of items in the specified slot of the inventory on the specified side of the adapter.""")
  def getSlotMaxStackSize(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
    InventoryUtils.inventoryAt(BlockPosition(host).offset(facing)) match {
      case Some(inventory) if checkPermission(inventory) =>
        result(Option(inventory.getStackInSlot(args.checkSlot(inventory, 1))).fold(0)(_.getMaxStackSize))
      case _ => result(Unit, "no inventory")
    }
  }

  @Callback(doc = """function(slotA:number, slotB:number):boolean -- Get whether the items in the two specified slots of the inventory on the specified side of the adapter are of the same type.""")
  def compareStacks(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
    InventoryUtils.inventoryAt(BlockPosition(host).offset(facing)) match {
      case Some(inventory) if checkPermission(inventory) =>
        val stackA = inventory.getStackInSlot(args.checkSlot(inventory, 1))
        val stackB = inventory.getStackInSlot(args.checkSlot(inventory, 2))
        result(haveSameItemType(stackA, stackB))
      case _ => result(Unit, "no inventory")
    }
  }

  @Callback(doc = """function(side:number, slot:number):table -- Get a description of the stack in the specified slot of the inventory on the specified side of the adapter.""")
  def getStackInSlot(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val facing = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
    InventoryUtils.inventoryAt(BlockPosition(host).offset(facing)) match {
      case Some(inventory) if checkPermission(inventory) =>
        result(inventory.getStackInSlot(args.checkSlot(inventory, 1)))
      case _ => result(Unit, "no inventory")
    }
  }
  else result(Unit, "not enabled in config")

  private def checkPermission(inventory: IInventory) = {
    fakePlayer synchronized {
      fakePlayer.setPosition(host.xPosition, host.yPosition, host.zPosition)
      inventory.isUseableByPlayer(fakePlayer)
    }
  }

  private def haveSameItemType(stackA: ItemStack, stackB: ItemStack) =
    (Option(stackA), Option(stackB)) match {
      case (Some(a), Some(b)) =>
        a.getItem == b.getItem &&
          (!a.getHasSubtypes || a.getItemDamage == b.getItemDamage)
      case (None, None) => true
      case _ => false
    }
}
