package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.BlockState
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

class Transposer(props: Properties) extends SimpleBlock(props) {
  override def newBlockEntity(world: IBlockReader) = new tileentity.Transposer(tileentity.TileEntityTypes.TRANSPOSER)
}
