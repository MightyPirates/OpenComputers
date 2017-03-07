package li.cil.oc.server.component.traits

import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.FluidUtil
import li.cil.oc.util.ResultWrapper.result
import net.minecraftforge.fluids.FluidStack

trait TankWorldControl extends TankAware with WorldAware with SideRestricted {
  @Callback(doc = "function(side:number):boolean -- Compare the fluid in the selected tank with the fluid on the specified side. Returns true if equal.")
  def compareFluid(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    fluidInTank(selectedTank) match {
      case Some(stack) =>
        FluidUtil.fluidHandlerAt(position.offset(side), side.getOpposite) match {
          case Some(handler) => result(Option(handler.getTankProperties).exists(_.exists(other => stack.isFluidEqual(other.getContents))))
          case _ => result(false)
        }
      case _ => result(false)
    }
  }

  @Callback(doc = "function(side:boolean[, amount:number=1000]):boolean, number or string -- Drains the specified amount of fluid from the specified side. Returns the amount drained, or an error message.")
  def drain(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optFluidCount(1) max 0
    getTank(selectedTank) match {
      case Some(tank) =>
        val space = tank.getCapacity - tank.getFluidAmount
        val amount = math.min(count, space)
        if (count < 1 || amount > 0) {
          FluidUtil.fluidHandlerAt(position.offset(facing), facing.getOpposite) match {
            case Some(handler) =>
              tank.getFluid match {
                case stack: FluidStack =>
                  val drained = handler.drain(new FluidStack(stack, amount), true)
                  if ((drained != null && drained.amount > 0) || amount == 0) {
                    val filled = tank.fill(drained, true)
                    result(true, filled)
                  }
                  else result(Unit, "incompatible or no fluid")
                case _ =>
                  val transferred = tank.fill(handler.drain(amount, true), true)
                  result(transferred > 0, transferred)
              }
            case _ => result(Unit, "incompatible or no fluid")
          }
        }
        else result(Unit, "tank is full")
      case _ => result(Unit, "no tank selected")
    }
  }

  @Callback(doc = "function(side:number[, amount:number=1000]):boolean, number of string -- Eject the specified amount of fluid to the specified side. Returns the amount ejected or an error message.")
  def fill(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optFluidCount(1) max 0
    getTank(selectedTank) match {
      case Some(tank) =>
        val amount = math.min(count, tank.getFluidAmount)
        if (count < 1 || amount > 0) {
          FluidUtil.fluidHandlerAt(position.offset(facing), facing.getOpposite) match {
            case Some(handler) =>
              tank.getFluid match {
                case stack: FluidStack =>
                  val filled = handler.fill(new FluidStack(stack, amount), true)
                  if (filled > 0 || amount == 0) {
                    tank.drain(filled, true)
                    result(true, filled)
                  }
                  else result(Unit, "incompatible or no fluid")
                case _ =>
                  result(Unit, "tank is empty")
              }
            case _ => result(Unit, "no space")
          }
        }
        else result(Unit, "tank is empty")
      case _ => result(Unit, "no tank selected")
    }
  }
}
