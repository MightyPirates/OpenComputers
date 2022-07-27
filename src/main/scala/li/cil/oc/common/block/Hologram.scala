package li.cil.oc.common.block

import java.util

import li.cil.oc.common.tileentity
import li.cil.oc.util.Rarity
import li.cil.oc.util.Tooltip
import net.minecraft.block.BlockState
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.IBlockReader
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.convert.ImplicitConversionsToScala._

class Hologram(val tier: Int) extends SimpleBlock {
  val bounds = new AxisAlignedBB(0, 0, 0, 1, 0.5f, 1)

  // ----------------------------------------------------------------------- //

  override def getBoundingBox(state: BlockState, world: IBlockReader, pos: BlockPos): AxisAlignedBB = bounds

  // ----------------------------------------------------------------------- //

  override def rarity(stack: ItemStack) = Rarity.byTier(tier)

  override protected def tooltipBody(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], advanced: ITooltipFlag) {
    for (curr <- Tooltip.get(getClass.getSimpleName.toLowerCase() + tier)) {
      tooltip.add(new StringTextComponent(curr))
    }
  }

  // ----------------------------------------------------------------------- //

  override def newBlockEntity(world: IBlockReader) = new tileentity.Hologram(tileentity.TileEntityTypes.HOLOGRAM, tier)
}
