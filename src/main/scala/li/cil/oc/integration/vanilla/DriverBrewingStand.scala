package li.cil.oc.integration.vanilla

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityBrewingStand
import net.minecraft.world.World

object DriverBrewingStand extends DriverTileEntity {
  override def getTileEntityClass: Class[_] = classOf[TileEntityBrewingStand]

  override def createEnvironment(world: World, x: Int, y: Int, z: Int): ManagedEnvironment =
    new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileEntityBrewingStand])

  final class Environment(tileEntity: TileEntityBrewingStand) extends ManagedTileEntityEnvironment[TileEntityBrewingStand](tileEntity, "brewing_stand") with NamedBlock {
    override def preferredName = "brewing_stand"

    override def priority = 0

    @Callback(doc = "function():number -- Get the number of ticks remaining of the current brewing operation.")
    def getBrewTime(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getBrewTime)
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (stack != null && stack.getItem == Items.brewing_stand)
        classOf[Environment]
      else null
    }
  }

}
