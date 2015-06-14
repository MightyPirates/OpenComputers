package li.cil.oc.integration.ec


import appeng.tile.misc.TileInterface
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.integration.appeng.AEUtil
import net.minecraft.item.ItemStack
import net.minecraft.world.World

object DriverInterface extends DriverTileEntity with EnvironmentAware {
  def getTileEntityClass: Class[_] = classOf[TileInterface]

  def createEnvironment(world: World, x: Int, y: Int, z: Int): ManagedEnvironment =
    new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileInterface])

  override def providedEnvironment(stack: ItemStack) =
    if (AEUtil.isBlockInterface(stack)) classOf[Environment]
    else null

  class Environment(val tile: TileInterface) extends ManagedTileEntityEnvironment[TileInterface](tile, "me_interface")  with NetworkControl[TileInterface] {
  }

}