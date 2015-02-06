package li.cil.oc.server.component.traits

import li.cil.oc.api.internal.MultiTank
import li.cil.oc.api.machine.Arguments
import li.cil.oc.util.ExtendedArguments._
import net.minecraftforge.fluids.FluidStack

trait TankAware {
  def tank: MultiTank

  def selectedTank: Int

  def selectedTank_=(value: Int): Unit

  // ----------------------------------------------------------------------- //

  protected def optTank(args: Arguments, n: Int) =
    if (args.count > 0 && args.checkAny(0) != null) args.checkTank(tank, 0)
    else selectedTank

  protected def getTank(index: Int) = Option(tank.getFluidTank(index))

  protected def fluidInTank(index: Int) = getTank(index) match {
    case Some(tank) => Option(tank.getFluid)
    case _ => None
  }

  protected def haveSameFluidType(stackA: FluidStack, stackB: FluidStack) = stackA.isFluidEqual(stackB)
}
