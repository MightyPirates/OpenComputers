package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.world.IBlockReader

class PowerConverter(props: Properties) extends SimpleBlock(props) with traits.PowerAcceptor {
  override def energyThroughput: Double = Settings.get.powerConverterRate

  override def newBlockEntity(world: IBlockReader) = new tileentity.PowerConverter(tileentity.TileEntityTypes.POWER_CONVERTER)
}
