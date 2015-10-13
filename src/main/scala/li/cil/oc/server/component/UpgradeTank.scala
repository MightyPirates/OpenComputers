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

  override def fill(stack: FluidStack, doFill: Boolean) = tank.fill(stack, doFill)

  override def drain(maxDrain: Int, doDrain: Boolean) = tank.drain(maxDrain, doDrain)
}
