package li.cil.oc.integration.minecraft

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.ComparatorTileEntity
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object DriverComparator extends DriverSidedTileEntity {
  override def getTileEntityClass: Class[_] = classOf[ComparatorTileEntity]

  override def createEnvironment(world: World, pos: BlockPos, side: Direction): ManagedEnvironment =
    new Environment(world.getBlockEntity(pos).asInstanceOf[ComparatorTileEntity])

  final class Environment(tileEntity: ComparatorTileEntity) extends ManagedTileEntityEnvironment[ComparatorTileEntity](tileEntity, "comparator") with NamedBlock {
    override def preferredName = "comparator"

    override def priority = 0

    @Callback(doc = "function():number -- Get the strength of the comparators output signal.")
    def getOutputSignal(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getOutputSignal)
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (!stack.isEmpty && stack.getItem == Items.COMPARATOR)
        classOf[Environment]
      else null
    }
  }

}
