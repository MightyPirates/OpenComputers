package li.cil.oc.server.component.traits

import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ResultWrapper.result

trait TankControl extends TankAware {
  @Callback
  def tankCount(context: Context, args: Arguments): Array[AnyRef] = result(tank.tankCount)

  @Callback
  def selectTank(context: Context, args: Arguments): Array[AnyRef] = {
    if (args.count > 0 && args.checkAny(0) != null) {
      selectedTank = args.checkTank(tank, 0)
    }
    result(selectedTank + 1)
  }

  @Callback(direct = true)
  def tankLevel(context: Context, args: Arguments): Array[AnyRef] = {
    val index =
      if (args.count > 0 && args.checkAny(0) != null) args.checkTank(tank, 0)
      else selectedTank
    result(fluidInTank(index) match {
      case Some(fluid) => fluid.amount
      case _ => 0
    })
  }

  @Callback(direct = true)
  def tankSpace(context: Context, args: Arguments): Array[AnyRef] = {
    val index =
      if (args.count > 0 && args.checkAny(0) != null) args.checkTank(tank, 0)
      else selectedTank
    result(getTank(index) match {
      case Some(tank) => tank.getCapacity - tank.getFluidAmount
      case _ => 0
    })
  }

  @Callback
  def compareFluidTo(context: Context, args: Arguments): Array[AnyRef] = {
    val index = args.checkTank(tank, 0)
    result((fluidInTank(selectedTank), fluidInTank(index)) match {
      case (Some(stackA), Some(stackB)) => haveSameFluidType(stackA, stackB)
      case (None, None) => true
      case _ => false
    })
  }

  @Callback
  def transferFluidTo(context: Context, args: Arguments): Array[AnyRef] = {
    val index = args.checkTank(tank, 0)
    val count = args.optionalFluidCount(1)
    if (index == selectedTank || count == 0) {
      result(true)
    }
    else (getTank(selectedTank), getTank(index)) match {
      case (Some(from), Some(to)) =>
        val drained = from.drain(count, false)
        val transferred = to.fill(drained, true)
        if (transferred > 0) {
          from.drain(transferred, true)
          result(true)
        }
        else if (count >= from.getFluidAmount && to.getCapacity >= from.getFluidAmount && from.getCapacity >= to.getFluidAmount) {
          // Swap.
          val tmp = to.drain(to.getFluidAmount, true)
          to.fill(from.drain(from.getFluidAmount, true), true)
          from.fill(tmp, true)
          result(true)
        }
        else result(Unit, "incompatible or no fluid")
      case _ => result(Unit, "invalid index")
    }
  }
}
