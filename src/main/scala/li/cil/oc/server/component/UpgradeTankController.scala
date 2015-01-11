package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.common.entity
import li.cil.oc.common.tileentity
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import net.minecraft.entity.Entity
import net.minecraft.util.EnumFacing

object UpgradeTankController {

  class Adapter(val host: EnvironmentHost) extends prefab.ManagedEnvironment with traits.WorldTankAnalytics {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("tank_controller", Visibility.Network).
      create()

    // ----------------------------------------------------------------------- //

    override def position = BlockPosition(host)

    override protected def checkSideForAction(args: Arguments, n: Int) = args.checkSide(n, EnumFacing.values: _*)
  }

  class Drone(val host: EnvironmentHost with entity.Drone) extends prefab.ManagedEnvironment with traits.TankInventoryControl with traits.WorldTankAnalytics {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("tank_controller", Visibility.Neighbors).
      create()

    override def position = BlockPosition(host: Entity)

    override def inventory = host.inventory

    override def selectedSlot = host.selectedSlot

    override def selectedSlot_=(value: Int) = host.selectedSlot = value

    override def tank = host.tank

    override def selectedTank = host.selectedTank

    override def selectedTank_=(value: Int) = host.selectedTank = value

    override protected def checkSideForAction(args: Arguments, n: Int) = args.checkSide(n, EnumFacing.values: _*)
  }

  class Robot(val host: EnvironmentHost with tileentity.Robot) extends prefab.ManagedEnvironment with traits.TankInventoryControl with traits.WorldTankAnalytics {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("tank_controller", Visibility.Neighbors).
      create()

    override def position = BlockPosition(host)

    override def inventory = host.dynamicInventory

    override def selectedSlot = host.selectedSlot - host.actualSlot(0)

    override def selectedSlot_=(value: Int) = host.selectedSlot = host.actualSlot(value)

    override def tank = host.tank

    override def selectedTank = host.selectedTank

    override def selectedTank_=(value: Int) = host.selectedTank = value

    override protected def checkSideForAction(args: Arguments, n: Int) = host.toGlobal(args.checkSideForAction(n))
  }

}
