package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

class Geolyzer(props: Properties) extends SimpleBlock(props) {
  override def newBlockEntity(world: IBlockReader) = new tileentity.Geolyzer(tileentity.TileEntityTypes.GEOLYZER)
}
