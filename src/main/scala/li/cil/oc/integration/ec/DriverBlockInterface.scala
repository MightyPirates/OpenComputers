package li.cil.oc.integration.ec


import appeng.api.implementations.tiles.ISegmentedInventory
import appeng.api.networking.IGridHost
import appeng.api.networking.security.IActionHost
import appeng.api.util.AEPartLocation
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.integration.appeng.AEUtil
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object DriverBlockInterface extends DriverSidedTileEntity {
  def getTileEntityClass: Class[_] = AEUtil.interfaceClass

  def createEnvironment(world: World, pos: BlockPos, side: EnumFacing): ManagedEnvironment =
    new Environment(world.getTileEntity(pos).asInstanceOf[TileEntity with ISegmentedInventory with IActionHost with IGridHost])

  final class Environment(val tile: TileEntity with ISegmentedInventory with IActionHost with IGridHost) extends ManagedTileEntityEnvironment[TileEntity with ISegmentedInventory with IActionHost](tile, "me_interface") with NetworkControl[TileEntity with ISegmentedInventory with IActionHost with IGridHost]{
    override def pos: AEPartLocation = AEPartLocation.INTERNAL
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (AEUtil.isBlockInterface(stack))
        classOf[Environment]
      else null
  }

}
