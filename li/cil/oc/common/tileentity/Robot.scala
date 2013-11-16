package li.cil.oc.common.tileentity

import li.cil.oc.Config
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network.{Context, Arguments, LuaCallback}
import li.cil.oc.server.component
import li.cil.oc.server.driver.Registry
import net.minecraft.item.ItemStack

class Robot(isClient: Boolean) extends Computer {
  def this() = this(false)

  // ----------------------------------------------------------------------- //

  val instance = if (isClient) null else new component.Computer(this)

  // ----------------------------------------------------------------------- //

  //def bounds =

  // ----------------------------------------------------------------------- //

  @LuaCallback("attack")
  def attack(context: Context, args: Arguments): Array[AnyRef] = {
    // Attack with equipped tool.
    val side = args.checkInteger(0)
    null
  }

  @LuaCallback("use")
  def use(context: Context, args: Arguments): Array[AnyRef] = {
    // Use equipped tool (e.g. dig, chop, till).
    val side = args.checkInteger(0)
    val sneaky = args.checkBoolean(1)
    null
  }

  // ----------------------------------------------------------------------- //

  @LuaCallback("check")
  def check(context: Context, args: Arguments): Array[AnyRef] = {
    // Test for blocks or entities.
    null
  }

  @LuaCallback("collect")
  def collect(context: Context, args: Arguments): Array[AnyRef] = {
    // Pick up items lying around.
    null
  }

  @LuaCallback("compare")
  def compare(context: Context, args: Arguments): Array[AnyRef] = {
    // Compare block to item selected in inventory.
    null
  }

  @LuaCallback("drop")
  def drop(context: Context, args: Arguments): Array[AnyRef] = {
    // Drop items from inventory.
    null
  }

  @LuaCallback("move")
  def move(context: Context, args: Arguments): Array[AnyRef] = {
    // Try to move in the specified direction.
    null
  }

  @LuaCallback("place")
  def place(context: Context, args: Arguments): Array[AnyRef] = {
    // Place block item selected in inventory.
    null
  }

  // ----------------------------------------------------------------------- //

  def getInvName = Config.namespace + "container.Robot"

  def getSizeInventory = 12

  def isItemValidForSlot(slot: Int, item: ItemStack) = (slot, Registry.driverFor(item)) match {
    case (0, Some(driver)) => driver.slot(item) == Slot.Tool
    case (1 | 2, Some(driver)) => driver.slot(item) == Slot.Card
    case (3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11, _) => true // Normal inventory.
    case _ => false // Invalid slot.
  }
}
