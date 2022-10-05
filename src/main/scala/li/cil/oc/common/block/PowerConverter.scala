package li.cil.oc.common.block

import java.text.DecimalFormat
import java.util

import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.util.Tooltip
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.item.ItemStack
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

class PowerConverter(props: Properties) extends SimpleBlock(props) with traits.PowerAcceptor {
  if (Settings.get.ignorePower) {
    ItemBlacklist.hide(this)
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput: Double = Settings.get.powerConverterRate

  override def newBlockEntity(world: IBlockReader) = new tileentity.PowerConverter(tileentity.TileEntityTypes.POWER_CONVERTER)
}
