package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.event.GeolyzerEvent
import li.cil.oc.api.event.GeolyzerEvent.Analyze
import li.cil.oc.api.internal.Rotatable
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.util.ExtendedArguments._
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._

class Geolyzer(val host: EnvironmentHost) extends prefab.ManagedEnvironment {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("geolyzer").
    withConnector().
    create()

  @Callback(doc = """function(x:number, z:number[, ignoreReplaceable:boolean|options:table]):table -- Analyzes the density of the column at the specified relative coordinates.""")
  def scan(computer: Context, args: Arguments): Array[AnyRef] = {
    val rx = args.checkInteger(0)
    val rz = args.checkInteger(1)
    val options = if (args.isBoolean(2)) mapAsJavaMap(Map("includeReplaceable" -> !args.checkBoolean(2))) else args.optTable(2, Map.empty[AnyRef, AnyRef])

    if (math.abs(rx) > Settings.get.geolyzerRange || math.abs(rz) > Settings.get.geolyzerRange) {
      throw new IllegalArgumentException("location out of bounds")
    }

    if (!node.tryChangeBuffer(-Settings.get.geolyzerScanCost))
      return result(Unit, "not enough energy")

    val event = new GeolyzerEvent.Scan(host, options, rx, rz)
    MinecraftForge.EVENT_BUS.post(event)
    if (event.isCanceled) result(Unit, "scan was canceled")
    else result(event.data)
  }

  @Callback(doc = """function(side:number[,options:table]):table -- Get some information on a directly adjacent block.""")
  def analyze(computer: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val side = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
    val localSide = host match {
      case rotatable: Rotatable => rotatable.toGlobal(side)
      case _ => side
    }
    val options = args.optTable(1, Map.empty[AnyRef, AnyRef])

    if (!node.tryChangeBuffer(-Settings.get.geolyzerScanCost))
      return result(Unit, "not enough energy")

    val event = new Analyze(host, options, localSide)
    MinecraftForge.EVENT_BUS.post(event)
    if (event.isCanceled) result(Unit, "scan was canceled")
    else result(event.data)
  }
  else result(Unit, "not enabled in config")
}
