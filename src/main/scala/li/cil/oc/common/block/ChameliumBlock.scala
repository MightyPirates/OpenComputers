package li.cil.oc.common.block

import li.cil.oc.util.Color
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.color.IBlockColor
import net.minecraft.item.EnumDyeColor
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

object ChameliumBlock {
  final val Color = PropertyEnum.create("color", classOf[EnumDyeColor])
}

class ChameliumBlock extends SimpleBlock(Material.ROCK) with IBlockColor {
  setDefaultState(blockState.getBaseState.withProperty(ChameliumBlock.Color, EnumDyeColor.BLACK))

  @SideOnly(Side.CLIENT)
  override def colorMultiplier(state: IBlockState, worldIn: IBlockAccess, pos: BlockPos, tintIndex: Int): Int = Color.rgbValues(Color.byOreName(Color.dyes(getMetaFromState(state) max 0 min Color.dyes.length)))

  override def damageDropped(state: IBlockState): Int = getMetaFromState(state)

  override def getStateFromMeta(meta: Int): IBlockState =
    getDefaultState.withProperty(ChameliumBlock.Color, EnumDyeColor.byDyeDamage(meta))

  override def getMetaFromState(state: IBlockState): Int =
    state.getValue(ChameliumBlock.Color).getDyeDamage

  override def createBlockState() = new BlockStateContainer(this, ChameliumBlock.Color)

  override def hasTileEntity(state: IBlockState): Boolean = false
}
