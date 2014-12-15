package li.cil.oc.server.component.traits

import li.cil.oc.common.inventory.MultiTank
import net.minecraftforge.fluids.FluidStack

trait TankAware {
  def tank: MultiTank

  def selectedTank: Int

  def selectedTank_=(value: Int): Unit

  // ----------------------------------------------------------------------- //

  protected def getTank(index: Int) = Option(tank.getFluidTank(index))

  protected def fluidInTank(index: Int) = getTank(index) match {
    case Some(tank) => Option(tank.getFluid)
    case _ => None
  }

  protected def haveSameFluidType(stackA: FluidStack, stackB: FluidStack) = stackA.isFluidEqual(stackB)
}
