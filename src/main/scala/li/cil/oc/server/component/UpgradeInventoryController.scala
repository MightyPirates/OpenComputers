package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.common.tileentity
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._

import scala.collection.convert.WrapAsJava._

object UpgradeInventoryController {

  trait Common extends DeviceInfo {
    private final lazy val deviceInfo = Map(
      DeviceAttribute.Class -> DeviceClass.Generic,
      DeviceAttribute.Description -> "Inventory controller",
      DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
      DeviceAttribute.Product -> "Item Cataloguer R1"
    )

    override def getDeviceInfo: util.Map[String, String] = deviceInfo
  }

  class Adapter(val host: EnvironmentHost) extends prefab.ManagedEnvironment with traits.WorldInventoryAnalytics with Common {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("inventory_controller", Visibility.Network).
      create()

    // ----------------------------------------------------------------------- //

    override def position = BlockPosition(host)

    override protected def checkSideForAction(args: Arguments, n: Int) = args.checkSideAny(n)
  }

  class Drone(val host: EnvironmentHost with internal.Agent) extends prefab.ManagedEnvironment with traits.InventoryAnalytics with traits.InventoryWorldControlMk2 with traits.WorldInventoryAnalytics with traits.ItemInventoryControl with Common {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("inventory_controller", Visibility.Neighbors).
      create()

    // ----------------------------------------------------------------------- //

    override def position = BlockPosition(host)

    override def inventory = host.mainInventory

    override def selectedSlot = host.selectedSlot

    override def selectedSlot_=(value: Int) = host.setSelectedSlot(value)

    override protected def checkSideForAction(args: Arguments, n: Int) = args.checkSideAny(n)
  }

  class Robot(val host: EnvironmentHost with tileentity.Robot) extends prefab.ManagedEnvironment with traits.InventoryAnalytics with traits.InventoryWorldControlMk2 with traits.WorldInventoryAnalytics with traits.ItemInventoryControl with Common {
    override val node = Network.newNode(this, Visibility.Network).
      withComponent("inventory_controller", Visibility.Neighbors).
      create()

    // ----------------------------------------------------------------------- //

    override def position = BlockPosition(host)

    override def inventory = host.mainInventory

    override def selectedSlot = host.selectedSlot

    override def selectedSlot_=(value: Int) = host.setSelectedSlot(value)

    override protected def checkSideForAction(args: Arguments, n: Int) = host.toGlobal(args.checkSideForAction(n))

    @Callback(doc = """function():boolean -- Swaps the equipped tool with the content of the currently selected inventory slot.""")
    def equip(context: Context, args: Arguments): Array[AnyRef] = {
      if (inventory.getSizeInventory > 0) {
        val equipped = host.getStackInSlot(0)
        val selected = inventory.getStackInSlot(selectedSlot)
        host.setInventorySlotContents(0, selected)
        inventory.setInventorySlotContents(selectedSlot, equipped)
        result(true)
      }
      else result(false)
    }
    @Callback(doc = """function([slot:number]):boolean -- Swaps the installed upgrade in the slot (1 by default) with the content of the currently selected inventory slot.""")
    def installUpgrade(context: Context, args: Arguments): Array[AnyRef] = {
      if (inventory.getSizeInventory > 0) {
        val slot = args.optInteger(0, 1)
        if (!host.isContainerSlot(slot))
          return result(false, "not a container slot")
        val selected = inventory.getStackInSlot(selectedSlot)
        if (selected != null && !host.isItemValidForSlot(slot, selected))
          return result(false, "Invalid upgrade")
        val equipped = host.getStackInSlot(slot)
        host.setInventorySlotContents(slot, selected)
        inventory.setInventorySlotContents(selectedSlot, equipped)
        result(true)
      }
      else result(false)
    }

    @Callback(doc = """function(slot:number):string -- get upgrade container type at the given slot.""")
    def getUpgradeContainerType(context: Context, args: Arguments): Array[AnyRef] = {
      val slot = args.checkInteger(0)
      if (!host.isContainerSlot(slot))
        return result("None")
      result(host.containerSlotType(slot))
    }

    @Callback(doc = """function(slot:number):number -- get upgrade container tier at the given slot.""")
    def getUpgradeContainerTier(context: Context, args: Arguments): Array[AnyRef] = {
      val slot = args.checkInteger(0)
      if (!host.isContainerSlot(slot))
        return result(0)
      result(host.containerSlotTier(slot))
    }
  }

}
