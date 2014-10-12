package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.internal.Adapter
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.util.ExtendedArguments._
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.IFluidHandler

class UpgradeTankControllerInAdapter(val host: EnvironmentHost with Adapter) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("tank_controller", Visibility.Network).
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(side:number):number -- Get the amount of fluid in the tank on the specified side of the adapter.""")
  def getTankLevel(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
    host.world.getTileEntity(math.floor(host.xPosition).toInt + facing.offsetX, math.floor(host.yPosition).toInt + facing.offsetY, math.floor(host.zPosition).toInt + facing.offsetZ) match {
      case handler: IFluidHandler =>
        result(handler.getTankInfo(facing.getOpposite).map(info => Option(info.fluid).fold(0)(_.amount)).sum)
      case _ => result(Unit, "no tank")
    }
  }

  @Callback(doc = """function(side:number):number -- Get the capacity of the tank on the specified side of the adapter.""")
  def getTankCapacity(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
    host.world.getTileEntity(math.floor(host.xPosition).toInt + facing.offsetX, math.floor(host.yPosition).toInt + facing.offsetY, math.floor(host.zPosition).toInt + facing.offsetZ) match {
      case handler: IFluidHandler =>
        result(handler.getTankInfo(facing.getOpposite).map(_.capacity).foldLeft(0)((max, capacity) => math.max(max, capacity)))
      case _ => result(Unit, "no tank")
    }
  }

  @Callback(doc = """function(side:number):table -- Get a description of the fluid in the the tank on the specified side of the adapter.""")
  def getFluidInTank(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val facing = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
    host.world.getTileEntity(math.floor(host.xPosition).toInt + facing.offsetX, math.floor(host.yPosition).toInt + facing.offsetY, math.floor(host.zPosition).toInt + facing.offsetZ) match {
      case handler: IFluidHandler => result(handler.getTankInfo(facing.getOpposite))
      case _ => result(Unit, "no tank")
    }
  }
  else result(Unit, "not enabled in config")
}
