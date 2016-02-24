package li.cil.oc.integration.thaumicenergistics

import appeng.api.networking.security.IActionHost
import appeng.me.helpers.IGridProxyable
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.util.ResultWrapper._
import net.minecraft.tileentity.TileEntity
import thaumicenergistics.api.IThEEssentiaGas

import scala.collection.convert.WrapAsScala._

// Note to self: this class is used by ExtraCells (and potentially others), do not rename / drastically change it.
trait NetworkControl[AETile >: Null <: TileEntity with IGridProxyable with IActionHost] {
  def tile: AETile

  @Callback(doc = "function():table -- Get a list of the stored essentia in the network.")
  def getEssentiaInNetwork(context: Context, args: Arguments): Array[AnyRef] =
    result(tile.getProxy.getStorage.getFluidInventory.getStorageList.filter(stack =>
      stack.getFluid != null && stack.getFluid.isInstanceOf[IThEEssentiaGas]).
      map(ThaumicEnergisticsUtils.getAspect).toArray)
}
