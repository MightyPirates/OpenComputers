package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.common.entity
import li.cil.oc.common.tileentity
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import net.minecraft.entity.Entity
import net.minecraftforge.common.util.ForgeDirection

object UpgradeInventoryController {

  class Adapter(val host: EnvironmentHost) extends prefab.ManagedEnvironment with traits.WorldInventoryAnalytics {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("inventory_controller", Visibility.Network).
      create()

    // ----------------------------------------------------------------------- //

    override def position = BlockPosition(host)

    override protected def checkSideForAction(args: Arguments, n: Int) = args.checkSide(n, ForgeDirection.VALID_DIRECTIONS: _*)
  }

  class Drone(val host: EnvironmentHost with entity.Drone) extends prefab.ManagedEnvironment with traits.InventoryAnalytics with traits.InventoryWorldControlMk2 with traits.WorldInventoryAnalytics {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("inventory_controller", Visibility.Neighbors).
      create()

    // ----------------------------------------------------------------------- //

    override def position = BlockPosition(host: Entity)

    override def inventory = host.inventory

    override def selectedSlot = host.selectedSlot

    override def selectedSlot_=(value: Int) = host.selectedSlot = value

    override protected def checkSideForAction(args: Arguments, n: Int) = args.checkSide(n, ForgeDirection.VALID_DIRECTIONS: _*)
  }

  class Robot(val host: EnvironmentHost with tileentity.Robot) extends prefab.ManagedEnvironment with traits.InventoryAnalytics with traits.InventoryWorldControlMk2 with traits.WorldInventoryAnalytics {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("inventory_controller", Visibility.Neighbors).
      create()

    // ----------------------------------------------------------------------- //

    override def position = BlockPosition(host)

    override def inventory = host.dynamicInventory

    override def selectedSlot = host.selectedSlot

    override def selectedSlot_=(value: Int) = host.selectedSlot = value

    override protected def checkSideForAction(args: Arguments, n: Int) = host.toGlobal(args.checkSideForAction(n))

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
  }

}
