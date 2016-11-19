package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.server.component.result
import li.cil.oc.util.FluidUtils

trait WorldTankAnalytics extends WorldAware with SideRestricted {
  @Callback(doc = """function(side:number):number -- Get the amount of fluid in the tank on the specified side.""")
  def getTankLevel(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)

    FluidUtils.fluidHandlerAt(position.offset(facing), facing.getOpposite) match {
      case Some(handler) =>
        result(handler.getTankInfo(facing.getOpposite).map(info => Option(info.fluid).fold(0)(_.amount)).sum)
      case _ => result(Unit, "no tank")
    }
  }

  @Callback(doc = """function(side:number):number -- Get the capacity of the tank on the specified side.""")
  def getTankCapacity(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    FluidUtils.fluidHandlerAt(position.offset(facing), facing.getOpposite) match {
      case Some(handler) =>
        result(handler.getTankInfo(facing.getOpposite).map(_.capacity).foldLeft(0)((max, capacity) => math.max(max, capacity)))
      case _ => result(Unit, "no tank")
    }
  }

  @Callback(doc = """function(side:number):table -- Get a description of the fluid in the the tank on the specified side.""")
  def getFluidInTank(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val facing = checkSideForAction(args, 0)
    FluidUtils.fluidHandlerAt(position.offset(facing), facing.getOpposite) match {
      case Some(handler) =>
        result(handler.getTankInfo(facing.getOpposite))
      case _ => result(Unit, "no tank")
    }
  }
  else result(Unit, "not enabled in config")
}
