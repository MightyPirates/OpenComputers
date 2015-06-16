package li.cil.oc.integration.ec

import appeng.api.networking.security.IActionHost
import appeng.me.helpers.IGridProxyable
import extracells.api.ECApi
import li.cil.oc.api.machine.{Arguments, Context, Callback}
import li.cil.oc.util.ResultWrapper._
import net.minecraft.tileentity.TileEntity
import scala.collection.convert.WrapAsScala._

// Note to self: this class is used by ExtraCells (and potentially others), do not rename / drastically change it.
trait NetworkControl[AETile >: Null <: TileEntity with IGridProxyable with IActionHost] {
  def tile: AETile

  val api = ECApi.instance

  @Callback(doc = "function():table -- Get a list of the stored gases in the network.")
  def getGasesInNetwork(context: Context, args: Arguments): Array[AnyRef] =
    result(tile.getProxy.getStorage.getFluidInventory.getStorageList.filter(stack => api.isGasStack(stack)).map(api.createGasStack(_)).toArray)


}