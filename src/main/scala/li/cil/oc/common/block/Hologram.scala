//package li.cil.oc.common.block
//
//import java.util
//
//import li.cil.oc.common.tileentity
//import li.cil.oc.util.{RarityUtils, Tooltip}
//import net.minecraft.block.state.IBlockState
//import net.minecraft.entity.player.EntityPlayer
//import net.minecraft.item.ItemStack
//import net.minecraft.util.EnumFacing
//import net.minecraft.util.math.AxisAlignedBB
//import net.minecraft.util.math.BlockPos
//import net.minecraft.world.IBlockAccess
//import net.minecraft.world.World
//import net.minecraftforge.fml.relauncher.Side
//import net.minecraftforge.fml.relauncher.SideOnly
//
//class Hologram(val tier: Int) extends AbstractBlock {
//  val bounds = new AxisAlignedBB(0, 0, 0, 1, 0.5f, 1)
//
//  // ----------------------------------------------------------------------- //
//
//  override def isOpaqueCube(state: IBlockState): Boolean = false
//
//  override def isFullCube(state: IBlockState): Boolean = false
//
//  override def isBlockSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = side == EnumFacing.DOWN
//
//  @SideOnly(Side.CLIENT)
//  override def shouldSideBeRendered(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) = {
//    super.shouldSideBeRendered(state, world, pos, side) || side == EnumFacing.UP
//  }
//
//  override def isSideSolid(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) = side == EnumFacing.DOWN
//
//  override def getBoundingBox(state: IBlockState, world: IBlockAccess, pos: BlockPos): AxisAlignedBB = bounds
//
//  // ----------------------------------------------------------------------- //
//
//  override def rarity(stack: ItemStack) = RarityUtils.fromTier(tier)
//
//  override protected def tooltipBody(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
//    tooltip.addAll(Tooltip.get(getClass.getSimpleName + tier))
//  }
//
//  // ----------------------------------------------------------------------- //
//
//  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Hologram(tier)
//}
