package li.cil.oc.integration.dsu

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper._
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import powercrystals.minefactoryreloaded.api.IDeepStorageUnit

object DriverDeepStorageUnit extends DriverSidedTileEntity {
  override def getTileEntityClass: Class[_] = classOf[IDeepStorageUnit]

  override def createEnvironment(world: World, x: Int, y: Int, z: Int, side: ForgeDirection): ManagedEnvironment =
    new Environment(world.getTileEntity(x, y, z).asInstanceOf[IDeepStorageUnit])

  final class Environment(tileEntity: IDeepStorageUnit) extends ManagedTileEntityEnvironment[IDeepStorageUnit](tileEntity, "deep_storage_unit") {
    @Callback(doc = "function():int -- Get the maximum number of stored items.")
    def getMaxStoredCount(context: Context, args: Arguments): Array[AnyRef] = result(tileEntity.getMaxStoredCount)

    @Callback(doc = "function():int -- Get the maximum number of stored items.")
    def getStoredCount(context: Context, args: Arguments): Array[AnyRef] = result(Option(tileEntity.getStoredItemType).fold(0)(_.stackSize))

    @Callback(doc = "function():int -- Get the maximum number of stored items.")
    def getStoredItemType(context: Context, args: Arguments): Array[AnyRef] = {
      if (Settings.get.allowItemStackInspection) result(tileEntity.getStoredItemType)
      else result(Unit, "not enabled in config")
    }
  }

}
