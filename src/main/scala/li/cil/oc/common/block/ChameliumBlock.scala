package li.cil.oc.common.block

import java.util.List

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.item.BlockItemUseContext
import net.minecraft.item.DyeColor
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.state.EnumProperty
import net.minecraft.state.StateContainer
import net.minecraft.util.NonNullList
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader

object ChameliumBlock {
  final val Color = EnumProperty.create("color", classOf[DyeColor])
}

class ChameliumBlock(props: Properties) extends SimpleBlock(props) {
  protected override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]): Unit = {
    builder.add(ChameliumBlock.Color)
  }
  registerDefaultState(stateDefinition.any.setValue(ChameliumBlock.Color, DyeColor.BLACK))

  override def getCloneItemStack(world: IBlockReader, pos: BlockPos, state: BlockState): ItemStack = {
    val stack = new ItemStack(this)
    stack.setDamageValue(state.getValue(ChameliumBlock.Color).getId)
    stack
  }

  override def getStateForPlacement(ctx: BlockItemUseContext): BlockState =
    defaultBlockState.setValue(ChameliumBlock.Color, DyeColor.byId(ctx.getItemInHand.getDamageValue))

  override def fillItemCategory(tab: ItemGroup, list: NonNullList[ItemStack]) {
    val stack = new ItemStack(this, 1)
    stack.setDamageValue(defaultBlockState.getValue(ChameliumBlock.Color).getId)
    list.add(stack)
  }
}
