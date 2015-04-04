package li.cil.oc.integration.fsp

import flaxbeard.steamcraft.tile.TileEntitySteamGauge
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.api.machine.{Callback, Context, Arguments}
import li.cil.oc.util.ResultWrapper._
import net.minecraft.world.World

import scala.language.existentials

object DriverSteamGauge extends DriverTileEntity{

  def getTileEntityClass: Class[_] = classOf[TileEntitySteamGauge]

  def createEnvironment(world: World, x: Int, y: Int, z: Int): ManagedEnvironment = new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileEntitySteamGauge])

  class Environment(tileEntity: TileEntitySteamGauge) extends ManagedTileEntityEnvironment[TileEntitySteamGauge](tileEntity, "steamGauge") with NamedBlock {
    override def preferredName = "steamGauge"

    override def priority = 0

    @Callback(doc = "function():number -- Returns the valves pressure.")
    def getPressure(context: Context, args: Arguments): Array[AnyRef] = result(tileEntity.getPressure)

    @Callback(doc = "function():boolean -- Returns the comparator output of the gauge.")
    def getComparatorValue(context: Context, args: Arguments): Array[AnyRef] = result(tileEntity.getComparatorOutput)

  }
}