package li.cil.oc.common.block.traits

import li.cil.oc.api
import net.minecraft.block.Block
import net.minecraft.util.BlockPos
import net.minecraft.world.World

trait StateAware extends Block {
  override def hasComparatorInputOverride = true

  override def getComparatorInputOverride(world: World, pos: BlockPos) =
    world.getTileEntity(pos) match {
      case stateful: api.util.StateAware =>
        if (stateful.getCurrentState.contains(api.util.StateAware.State.IsWorking)) 15
        else if (stateful.getCurrentState.contains(api.util.StateAware.State.CanWork)) 10
        else 0
      case _ => 0
    }
}
