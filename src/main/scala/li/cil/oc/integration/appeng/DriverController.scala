package li.cil.oc.integration.appeng

import appeng.api.networking.security.IActionHost
import appeng.me.helpers.IGridProxyable
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

import scala.language.existentials

object DriverController extends DriverTileEntity with EnvironmentAware {
  private type TileController = TileEntity with IGridProxyable with IActionHost

  def getTileEntityClass = AEUtil.controllerClass

  def createEnvironment(world: World, x: Int, y: Int, z: Int): ManagedEnvironment =
    new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileController])

  override def providedEnvironment(stack: ItemStack) =
    if (AEUtil.isController(stack)) classOf[Environment]
    else null

  class Environment(val tile: TileController) extends ManagedTileEntityEnvironment[TileController](tile, "me_controller") with NamedBlock with NetworkControl[TileController] {
    override def preferredName = "me_controller"

    override def priority = 5
  }

}
