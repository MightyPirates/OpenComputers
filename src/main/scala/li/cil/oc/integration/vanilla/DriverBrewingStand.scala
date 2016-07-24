package li.cil.oc.integration.vanilla

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityBrewingStand
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object DriverBrewingStand extends DriverSidedTileEntity {
  override def getTileEntityClass: Class[_] = classOf[TileEntityBrewingStand]

  override def createEnvironment(world: World, pos: BlockPos, side: EnumFacing): ManagedEnvironment =
    new Environment(world.getTileEntity(pos).asInstanceOf[TileEntityBrewingStand])

  final class Environment(tileEntity: TileEntityBrewingStand) extends ManagedTileEntityEnvironment[TileEntityBrewingStand](tileEntity, "brewing_stand") with NamedBlock {
    override def preferredName = "brewing_stand"

    override def priority = 0

    @Callback(doc = "function():number -- Get the number of ticks remaining of the current brewing operation.")
    def getBrewTime(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getField(0))
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (stack != null && stack.getItem == Items.BREWING_STAND)
        classOf[Environment]
      else null
    }
  }

}
