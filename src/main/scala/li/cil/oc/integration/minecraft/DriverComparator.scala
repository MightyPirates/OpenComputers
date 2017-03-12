package li.cil.oc.integration.minecraft

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.{EnvironmentItem, ManagedEnvironment, NodeContainerItem}
import li.cil.oc.api.prefab.driver.AbstractDriverTileEntity
import li.cil.oc.integration.{ManagedTileEntityEnvironment, ManagedTileEntityNodeContainer}
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityComparator
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object DriverComparator extends AbstractDriverTileEntity {
  override def getTileEntityClass: Class[_] = classOf[TileEntityComparator]

  override def createEnvironment(world: World, pos: BlockPos, side: EnumFacing): NodeContainerItem =
    new NodeContainer(world.getTileEntity(pos).asInstanceOf[TileEntityComparator])

  final class NodeContainer(tileEntity: TileEntityComparator) extends ManagedTileEntityNodeContainer[TileEntityComparator](tileEntity, "comparator") with NamedBlock {
    override def preferredName = "comparator"

    override def priority = 0

    @Callback(doc = "function():number -- Get the strength of the comparators output signal.")
    def getOutputSignal(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getOutputSignal)
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (stack != null && stack.getItem == Items.COMPARATOR)
        classOf[NodeContainer]
      else null
    }
  }

}
