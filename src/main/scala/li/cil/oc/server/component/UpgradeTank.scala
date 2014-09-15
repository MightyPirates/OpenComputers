package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.driver.Container
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.component.ManagedComponent
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fluids.{FluidStack, FluidTank, IFluidTank}

class UpgradeTank(val owner: Container,val capacity:Int) extends ManagedComponent with IFluidTank {
  val node = Network.newNode(this, Visibility.Network).
    withConnector().
    create()

  val tank = new FluidTank(capacity)


  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    tank.writeToNBT(nbt)
  }

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    tank.readFromNBT(nbt)
  }

  override def getFluid = tank.getFluid

  override def getFluidAmount = tank.getFluidAmount

  override def getCapacity = tank.getCapacity

  override def getInfo = tank.getInfo

  override def fill(stack: FluidStack, doFill: Boolean) = tank.fill(stack, doFill)

  override def drain(maxDrain: Int, doDrain: Boolean) = tank.drain(maxDrain, doDrain)
}
