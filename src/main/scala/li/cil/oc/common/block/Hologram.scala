package li.cil.oc.common.block

import java.util

import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import li.cil.oc.integration.coloredlights.ModColoredLights
import li.cil.oc.util.Rarity
import li.cil.oc.util.Tooltip
import _root_.net.minecraft.entity.player.EntityPlayer
import _root_.net.minecraft.item.ItemStack
import _root_.net.minecraft.util.EnumFacing
import _root_.net.minecraft.util.math.BlockPos
import _root_.net.minecraft.world.IBlockAccess
import _root_.net.minecraft.world.World
import _root_.net.minecraftforge.fml.relauncher.Side
import _root_.net.minecraftforge.fml.relauncher.SideOnly
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.AxisAlignedBB

class Hologram(val tier: Int) extends SimpleBlock {
  if (Settings.get.hologramLight) {
    ModColoredLights.setLightLevel(this, 15, 15, 15)
  }

  val bounds = new AxisAlignedBB(0, 0, 0, 1, 0.5f, 1)

  // ----------------------------------------------------------------------- //

  override def isOpaqueCube(state: IBlockState): Boolean = false

  override def isFullCube(state: IBlockState): Boolean = false

  override def isBlockSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = side == EnumFacing.DOWN

  @SideOnly(Side.CLIENT)
  override def shouldSideBeRendered(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) = {
    super.shouldSideBeRendered(state, world, pos, side) || side == EnumFacing.UP
  }

  override def isSideSolid(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) = side == EnumFacing.DOWN

  override def getBoundingBox(state: IBlockState, world: IBlockAccess, pos: BlockPos): AxisAlignedBB = bounds

  // ----------------------------------------------------------------------- //

  override def rarity(stack: ItemStack) = Rarity.byTier(tier)

  override protected def tooltipBody(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(getClass.getSimpleName + tier))
  }

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Hologram(tier)
}
