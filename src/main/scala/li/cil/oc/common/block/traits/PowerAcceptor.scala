package li.cil.oc.common.block.traits

import java.util

import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

trait PowerAcceptor extends SimpleBlock {
  def energyThroughput: Double

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    super.tooltipTail(metadata, stack, player, tooltip, advanced)
    tooltip.addAll(Tooltip.extended("PowerAcceptor", energyThroughput.toInt))
  }
}
