package li.cil.oc.integration.gc

import li.cil.oc.api.Network
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.server.component.result
import micdoodle8.mods.galacticraft.api.world.IAtmosphericGas
import micdoodle8.mods.galacticraft.api.world.IGalacticraftWorldProvider

class WorldSensorCard(val host: EnvironmentHost) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Neighbors).
    withComponent("world_sensor").
    withConnector().
    create()

  @Callback(doc = """function():number -- Get the gravity of the world the device is currently in.""")
  def getGravity(context: Context, args: Arguments): Array[AnyRef] =
    withProvider(provider => result(provider.getGravity))(result(1f))

  @Callback(doc = """function():boolean -- Get whether the world the device is currently in has a breathable atmosphere.""")
  def hasBreathableAtmosphere(context: Context, args: Arguments): Array[AnyRef] =
    withProvider(provider => result(provider.hasBreathableAtmosphere))(result(true))

  @Callback(doc = """function(gas:string):boolean -- Get whether the world the device is currently in has the specified gas (e.g. oxygen or nitrogen).""")
  def isGasPresent(context: Context, args: Arguments): Array[AnyRef] =
    withProvider(provider => {
      val gas = IAtmosphericGas.valueOf(args.checkString(0).toUpperCase)
      result(provider.isGasPresent(gas))
    })(result(true))

  @Callback(doc = """function():number -- Get the wind level in the world the device is currently in.""")
  def getWindLevel(context: Context, args: Arguments): Array[AnyRef] =
    withProvider(provider => result(provider.getWindLevel))(result(1f))

  private def withProvider(f: IGalacticraftWorldProvider => Array[AnyRef])(default: Array[AnyRef] = null) = host.world.provider match {
    case provider: IGalacticraftWorldProvider => f(provider)
    case _ => default
  }
}
