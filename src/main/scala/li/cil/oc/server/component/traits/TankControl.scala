package li.cil.oc.server.component.traits

import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ResultWrapper.result

trait TankControl extends TankAware {
  @Callback(doc = "function():number -- The number of tanks installed in the device.")
  def tankCount(context: Context, args: Arguments): Array[AnyRef] = result(tank.tankCount)

  @Callback(doc = "function([index:number]):number -- Select a tank and/or get the number of the currently selected tank.")
  def selectTank(context: Context, args: Arguments): Array[AnyRef] = {
    if (args.count > 0 && args.checkAny(0) != null) {
      selectedTank = args.checkTank(tank, 0)
    }
    result(selectedTank + 1)
  }

  @Callback(direct = true, doc = "function([index:number]):number -- Get the fluid amount in the specified or selected tank.")
  def tankLevel(context: Context, args: Arguments): Array[AnyRef] = {
    val index =
      if (args.count > 0 && args.checkAny(0) != null) args.checkTank(tank, 0)
      else selectedTank
    result(fluidInTank(index) match {
      case Some(fluid) => fluid.amount
      case _ => 0
    })
  }

  @Callback(direct = true, doc = "function([index:number]):number -- Get the remaining fluid capacity in the specified or selected tank.")
  def tankSpace(context: Context, args: Arguments): Array[AnyRef] = {
    val index =
      if (args.count > 0 && args.checkAny(0) != null) args.checkTank(tank, 0)
      else selectedTank
    result(getTank(index) match {
      case Some(tank) => tank.getCapacity - tank.getFluidAmount
      case _ => 0
    })
  }

  @Callback(doc = "function(index:number):boolean -- Compares the fluids in the selected and the specified tank. Returns true if equal.")
  def compareFluidTo(context: Context, args: Arguments): Array[AnyRef] = {
    val index = args.checkTank(tank, 0)
    result((fluidInTank(selectedTank), fluidInTank(index)) match {
      case (Some(stackA), Some(stackB)) => haveSameFluidType(stackA, stackB)
      case (None, None) => true
      case _ => false
    })
  }

  @Callback(doc = "function(index:number[, count:number=1000]):boolean -- Move the specified amount of fluid from the selected tank into the specified tank.")
  def transferFluidTo(context: Context, args: Arguments): Array[AnyRef] = {
    val index = args.checkTank(tank, 0)
    val count = args.optFluidCount(1)
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
