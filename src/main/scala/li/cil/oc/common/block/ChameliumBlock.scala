package li.cil.oc.common.block

import li.cil.oc.util.Color
import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.state.BlockState
import net.minecraft.block.state.IBlockState
import net.minecraft.item.EnumDyeColor
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

object ChameliumBlock {
  final val Color = PropertyEnum.create("color", classOf[EnumDyeColor])
}

class ChameliumBlock extends SimpleBlock(Material.rock) {
  setDefaultState(blockState.getBaseState.withProperty(ChameliumBlock.Color, EnumDyeColor.BLACK))

  @SideOnly(Side.CLIENT)
  override def getRenderColor(state: IBlockState): Int = Color.rgbValues(Color.byOreName(Color.dyes(getMetaFromState(state) max 0 min Color.dyes.length)))

  override def damageDropped(state: IBlockState): Int = getMetaFromState(state)

  override def getStateFromMeta(meta: Int): IBlockState =
    getDefaultState.withProperty(ChameliumBlock.Color, EnumDyeColor.byDyeDamage(meta))

  override def getMetaFromState(state: IBlockState): Int =
    state.getValue(ChameliumBlock.Color).getDyeDamage

  override def createBlockState(): BlockState = new BlockState(this, ChameliumBlock.Color)

  override def hasTileEntity(state: IBlockState): Boolean = false
}
