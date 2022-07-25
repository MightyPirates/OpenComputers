package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import net.minecraft.nbt.CompoundNBT
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction
import net.minecraftforge.fluids.capability.templates.FluidTank

import scala.collection.convert.WrapAsJava._

class UpgradeTank(val owner: EnvironmentHost, val capacity: Int) extends AbstractManagedEnvironment with IFluidTank with DeviceInfo {
  override val node = Network.newNode(this, Visibility.None).create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Tank upgrade",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Superblubb V10",
    DeviceAttribute.Capacity -> capacity.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  val tank = new FluidTank(capacity)

  override def loadData(nbt: CompoundNBT) {
    super.loadData(nbt)
    tank.readFromNBT(nbt)
  }

  override def saveData(nbt: CompoundNBT) {
    super.saveData(nbt)
    tank.writeToNBT(nbt)
  }

  // ----------------------------------------------------------------------- //

  override def getFluid = tank.getFluid

  override def getFluidAmount = tank.getFluidAmount

  override def getCapacity = tank.getCapacity

  override def isFluidValid(stack: FluidStack) = tank.isFluidValid(stack)

  override def fill(stack: FluidStack, action: FluidAction) = {
    val amount = tank.fill(stack, action)
    if (action.execute && amount > 0) {
      node.sendToVisible("computer.signal", "tank_changed", Int.box(tankIndex), Int.box(amount))
    }
    amount
  }

  override def drain(stack: FluidStack, action: FluidAction) = {
    val amount = tank.drain(stack, action)
    if (action.execute && amount != null && amount.getAmount > 0) {
      node.sendToVisible("computer.signal", "tank_changed", Int.box(tankIndex), Int.box(-amount.getAmount))
    }
    amount
  }

  override def drain(maxDrain: Int, action: FluidAction) = {
    val amount = tank.drain(maxDrain, action)
    if (action.execute && amount != null && amount.getAmount > 0) {
      node.sendToVisible("computer.signal", "tank_changed", Int.box(tankIndex), Int.box(-amount.getAmount))
    }
    amount
  }

  private def tankIndex = {
    owner match {
      case agent: li.cil.oc.api.internal.Agent if agent.tank != null =>
        val tanks = (0 until agent.tank.tankCount).map(agent.tank.getFluidTank)
        val index = tanks.indexOf(this)
        (index max 0) + 1
      case _ => 1
    }
  }
}
