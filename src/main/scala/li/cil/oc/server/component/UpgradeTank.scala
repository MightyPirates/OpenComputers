package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidTank
import net.minecraftforge.fluids.IFluidTank

class UpgradeTank(val owner: EnvironmentHost, val capacity: Int) extends prefab.ManagedEnvironment with IFluidTank {
  override val node = Network.newNode(this, Visibility.None).create()

  val tank = new FluidTank(capacity)

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    tank.readFromNBT(nbt)
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    tank.writeToNBT(nbt)
  }

  // ----------------------------------------------------------------------- //

  override def getFluid = tank.getFluid

  override def getFluidAmount = tank.getFluidAmount

  override def getCapacity = tank.getCapacity

  override def getInfo = tank.getInfo

  override def fill(stack: FluidStack, doFill: Boolean) = {
    val amount = tank.fill(stack, doFill)
    if (doFill && amount > 0) {
      node.sendToVisible("computer.signal", "tank_changed", Int.box(tankIndex), Int.box(amount))
    }
    amount
  }

  override def drain(maxDrain: Int, doDrain: Boolean) = {
    val amount = tank.drain(maxDrain, doDrain)
    if (doDrain && amount != null && amount.amount > 0) {
      node.sendToVisible("computer.signal", "tank_changed", Int.box(tankIndex), Int.box(-amount.amount))
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
