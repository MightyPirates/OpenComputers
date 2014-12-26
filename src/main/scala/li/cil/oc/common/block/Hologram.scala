package li.cil.oc.common.block

import java.util

import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemStack
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraft.util.EnumFacing

class Hologram(val tier: Int) extends SimpleBlock with traits.SpecialBlock {
  setLightLevel(1)
  setBlockBounds(0, 0, 0, 1, 0.5f, 1)

  // ----------------------------------------------------------------------- //

  override def isBlockSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = side == EnumFacing.DOWN

  @SideOnly(Side.CLIENT)
  override def shouldSideBeRendered(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = {
    super.shouldSideBeRendered(world, pos, side) || side == EnumFacing.UP
  }

  // ----------------------------------------------------------------------- //

  override def rarity = Array(EnumRarity.UNCOMMON, EnumRarity.RARE).apply(tier)

  override protected def tooltipBody(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(getClass.getSimpleName + tier))
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(state: IBlockState) = true

  override def createTileEntity(world: World, state: IBlockState) = new tileentity.Hologram(tier)
}
