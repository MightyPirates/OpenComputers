package li.cil.oc.integration.appeng

import appeng.api.networking.IGridHost
import appeng.api.networking.security.IActionHost
import appeng.api.util.AEPartLocation
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

import scala.language.existentials

object DriverController extends DriverSidedTileEntity {
  private type TileController = TileEntity with IActionHost

  def getTileEntityClass = AEUtil.controllerClass

  def createEnvironment(world: World, pos: BlockPos, side: EnumFacing): ManagedEnvironment =
    new Environment(world.getTileEntity(pos).asInstanceOf[TileController])

  final class Environment(val tile: TileController) extends ManagedTileEntityEnvironment[TileController](tile, "me_controller") with NamedBlock with NetworkControl[TileController] {
    override def preferredName = "me_controller"

    override def pos: AEPartLocation = AEPartLocation.INTERNAL

    override def priority = 5
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (AEUtil.isController(stack))
        classOf[Environment]
      else null
  }

}
