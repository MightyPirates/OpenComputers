package li.cil.oc.integration.thaumicenergistics

import appeng.api.networking.security.IActionHost
import appeng.me.helpers.IGridProxyable
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.integration.appeng.AEUtil
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.world.World

import scala.language.existentials

object DriverController extends DriverSidedTileEntity {
  private type TileController = TileEntity with IGridProxyable with IActionHost

  def getTileEntityClass = AEUtil.controllerClass

  def createEnvironment(world: World, x: Int, y: Int, z: Int, side: EnumFacing): ManagedEnvironment =
    new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileController])

  final class Environment(val tile: TileController) extends ManagedTileEntityEnvironment[TileController](tile, "me_controller") with NetworkControl[TileController]

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (AEUtil.isController(stack))
        classOf[Environment]
      else null
  }

}
