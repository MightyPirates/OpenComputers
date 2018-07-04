package li.cil.oc.common.block.traits

import java.util

import li.cil.oc.common.block.SimpleBlock
import li.cil.oc.util.Tooltip
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

trait PowerAcceptor extends SimpleBlock {
  def energyThroughput: Double

  // ----------------------------------------------------------------------- //

  override protected def tooltipTail(metadata: Int, stack: ItemStack, world: World, tooltip: util.List[String], advanced: ITooltipFlag) {
    super.tooltipTail(metadata, stack, world, tooltip, advanced)
    tooltip.addAll(Tooltip.extended("PowerAcceptor", energyThroughput.toInt))
  }
}
