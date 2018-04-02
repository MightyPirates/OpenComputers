package li.cil.oc.integration.enderstorage

import codechicken.enderstorage.common.TileFrequencyOwner
import li.cil.oc.api.machine.{Arguments, Callback, Context, Machine}
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

final class DriverFrequencyOwner extends DriverSidedTileEntity {
  override def getTileEntityClass = classOf[TileFrequencyOwner]
  override def createEnvironment(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) =
    new DriverFrequencyOwner.Environment(world.getTileEntity(x, y, z).asInstanceOf[TileFrequencyOwner])
}

object DriverFrequencyOwner {
  final class Environment(val enderDevice: TileFrequencyOwner)
    extends ManagedTileEntityEnvironment[TileFrequencyOwner](enderDevice, "ender_storage") {

    @Callback(doc = "function():number -- Get the currently set frequency.")
    def getFrequency(context: Context, args: Arguments): Array[AnyRef] = Array[AnyRef](Int.box(enderDevice.freq))

    @Callback(doc = "function(value:number) -- Set the frequency. Who would have thought?!")
    def setFrequency(context: Context, args: Arguments): Array[AnyRef] = {
      val frequency = args.checkInteger(0)
      if ((frequency & 0xFFF) != frequency)
        throw new IllegalArgumentException("invalid frequency")

      import li.cil.oc.Settings
      import Settings.EnderStorageFrequencyPolicy._
      val error = (enderDevice.owner, Settings.get.enderStorageFrequencyPolicy) match {
        case (_, Always) | (null | "" | "global", _) => None
        case (owner, DeviceOwnerIsSingleOwner) => context match {
          case machine: Machine if machine.users sameElements Array(owner) => None
          case _ => Some("the owner of the Ender device must be the single computer owner")
        }
        case _ => Some("cannot change frequency of owned storage")
      }
      error match {
        case None =>
          enderDevice.setFreq(frequency)
          null
        case Some(msg) => Array[AnyRef](null, msg)
      }
    }

    @Callback(doc = "function():string -- Get the name of the owner, which is usually a player's name or 'global'.")
    def getOwner(context: Context, args: Arguments): Array[AnyRef] = Array[AnyRef](enderDevice.owner)
  }
}
