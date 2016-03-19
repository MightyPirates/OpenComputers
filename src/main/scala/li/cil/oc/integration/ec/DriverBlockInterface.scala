package li.cil.oc.integration.ec

import appeng.tile.misc.TileInterface
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.integration.appeng.AEUtil
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

object DriverBlockInterface extends DriverSidedTileEntity {
  def getTileEntityClass: Class[_] = classOf[TileInterface]

  def createEnvironment(world: World, x: Int, y: Int, z: Int, side: ForgeDirection): ManagedEnvironment =
    new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileInterface])

  final class Environment(val tile: TileInterface) extends ManagedTileEntityEnvironment[TileInterface](tile, "me_interface") with NetworkControl[TileInterface]

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (AEUtil.isBlockInterface(stack))
        classOf[Environment]
      else null
  }

}
