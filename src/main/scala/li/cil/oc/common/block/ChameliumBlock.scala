package li.cil.oc.common.block

import net.minecraft.block.material.Material
import net.minecraft.block.properties.PropertyEnum
import net.minecraft.block.state.BlockStateContainer
import net.minecraft.block.state.IBlockState
import net.minecraft.item.EnumDyeColor

object ChameliumBlock {
  final val Color = PropertyEnum.create("color", classOf[EnumDyeColor])
}

class ChameliumBlock extends SimpleBlock(Material.ROCK) {
  setDefaultState(blockState.getBaseState.withProperty(ChameliumBlock.Color, EnumDyeColor.BLACK))

  override def damageDropped(state: IBlockState): Int = getMetaFromState(state)

  override def getStateFromMeta(meta: Int): IBlockState =
    getDefaultState.withProperty(ChameliumBlock.Color, EnumDyeColor.byDyeDamage(meta))

  override def getMetaFromState(state: IBlockState): Int =
    state.getValue(ChameliumBlock.Color).getDyeDamage

  override def createBlockState() = new BlockStateContainer(this, ChameliumBlock.Color)

  override def hasTileEntity(state: IBlockState): Boolean = false
}
