package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.server.component.result
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.FluidUtils
import net.minecraftforge.fluids.FluidTankInfo

trait WorldTankAnalytics extends WorldAware with SideRestricted {
  @Callback(doc = """function(side:number [, tank:number]):number -- Get the amount of fluid in the specified tank on the specified side.""")
  def getTankLevel(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)

    FluidUtils.fluidHandlerAt(position.offset(facing), facing.getOpposite) match {
      case Some(handler) => args.optTankInfo(handler, facing.getOpposite, 1, null) match {
        case info: FluidTankInfo => result(Option(info.fluid).fold(0)(_.amount))
        case _ => result(handler.getTankInfo(facing.getOpposite).map(info => Option(info.fluid).fold(0)(_.amount)).sum)
      }
      case _ => result(Unit, "no tank")
    }
  }

  @Callback(doc = """function(side:number [, tank:number]):number -- Get the capacity of the specified tank on the specified side.""")
  def getTankCapacity(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    FluidUtils.fluidHandlerAt(position.offset(facing), facing.getOpposite) match {
      case Some(handler) => args.optTankInfo(handler, facing.getOpposite, 1, null) match {
        case info: FluidTankInfo => result(info.capacity)
        case _ => result(handler.getTankInfo(facing.getOpposite).map(_.capacity).foldLeft(0)((max, capacity) => math.max(max, capacity)))
      }
      case _ => result(Unit, "no tank")
    }
  }

  @Callback(doc = """function(side:number [, tank:number]):table -- Get a description of the fluid in the the specified tank on the specified side.""")
  def getFluidInTank(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val facing = checkSideForAction(args, 0)
    FluidUtils.fluidHandlerAt(position.offset(facing), facing.getOpposite) match {
      case Some(handler) => args.optTankInfo(handler, facing.getOpposite, 1, null) match {
        case info: FluidTankInfo => result(info)
        case _ => result(handler.getTankInfo(facing.getOpposite))
      }
      case _ => result(Unit, "no tank")
    }
  }
  else result(Unit, "not enabled in config")

  @Callback(doc = """function(side:number):number -- Get the number of tanks available on the specified side.""")
  def getTankCount(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    FluidUtils.fluidHandlerAt(position.offset(facing), facing.getOpposite) match {
      case Some(handler) => result(handler.getTankInfo(facing.getOpposite).length)
      case _ => result(Unit, "no tank")
    }
  }
}
