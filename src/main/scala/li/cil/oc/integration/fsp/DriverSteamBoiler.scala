package li.cil.oc.integration.fsp

import flaxbeard.steamcraft.tile.TileEntityBoiler
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.api.machine.{Callback, Context, Arguments}
import li.cil.oc.util.ResultWrapper._
import net.minecraft.world.World

import scala.language.existentials

object DriverSteamBoiler extends DriverTileEntity{

  def getTileEntityClass: Class[_] = classOf[TileEntityBoiler]

  def createEnvironment(world: World, x: Int, y: Int, z: Int): ManagedEnvironment = new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileEntityBoiler])

  class Environment(tileEntity: TileEntityBoiler) extends ManagedTileEntityEnvironment[TileEntityBoiler](tileEntity, "boiler") with NamedBlock {
    override def preferredName = "boiler"

    override def priority = 0

    @Callback(doc = "function():number -- Returns the boilers pressure.")
    def getPressure(context: Context, args: Arguments): Array[AnyRef] = result(tileEntity.getPressure)
  }

}
