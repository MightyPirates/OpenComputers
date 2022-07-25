package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.common.tileentity
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._

import scala.collection.convert.ImplicitConversionsToJava._

object UpgradeTankController {

  trait Common extends DeviceInfo {
    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Generic,
      DeviceAttribute.Description -> "Tank controller",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "FlowCheckDX"
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo
  }

  class Adapter(val host: EnvironmentHost) extends AbstractManagedEnvironment with traits.WorldTankAnalytics with Common {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("tank_controller", Visibility.Network).
      create()

    // ----------------------------------------------------------------------- //

    override def position = BlockPosition(host)

    override protected def checkSideForAction(args: Arguments, n: Int) = args.checkSideAny(n)
  }

  class Drone(val host: EnvironmentHost with internal.Agent) extends AbstractManagedEnvironment with traits.TankInventoryControl with traits.WorldTankAnalytics with Common {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("tank_controller", Visibility.Neighbors).
      create()

    override def position = BlockPosition(host)

    override def inventory = host.mainInventory

    override def selectedSlot = host.selectedSlot

    override def selectedSlot_=(value: Int) = host.setSelectedSlot(value)

    override def tank = host.tank

    override def selectedTank = host.selectedTank

    override def selectedTank_=(value: Int) = host.setSelectedTank(value)

    override protected def checkSideForAction(args: Arguments, n: Int) = args.checkSideAny(n)
  }

  class Robot(val host: EnvironmentHost with tileentity.Robot) extends AbstractManagedEnvironment with traits.TankInventoryControl with traits.WorldTankAnalytics with Common {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("tank_controller", Visibility.Neighbors).
      create()

    override def position = BlockPosition(host)

    override def inventory = host.mainInventory

    override def selectedSlot = host.selectedSlot

    override def selectedSlot_=(value: Int) = host.setSelectedSlot(value)

    override def tank = host.tank

    override def selectedTank = host.selectedTank

    override def selectedTank_=(value: Int) = host.selectedTank = value

    override protected def checkSideForAction(args: Arguments, n: Int) = host.toGlobal(args.checkSideForAction(n))
  }

}
