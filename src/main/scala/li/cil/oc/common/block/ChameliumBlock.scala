package li.cil.oc.common.block

import java.util.Collections
import java.util.List

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.AbstractBlock.Properties
import net.minecraft.block.material.Material
import net.minecraft.state.EnumProperty
import net.minecraft.item.DyeColor
import net.minecraft.item.ItemStack
import net.minecraft.loot.LootContext
import net.minecraft.state.StateContainer

object ChameliumBlock {
  final val Color = EnumProperty.create("color", classOf[DyeColor])
}

class ChameliumBlock(props: Properties = Properties.of(Material.STONE).strength(2, 5)) extends SimpleBlock(props) {
  protected override def createBlockStateDefinition(builder: StateContainer.Builder[Block, BlockState]): Unit = {
    builder.add(ChameliumBlock.Color)
  }
  registerDefaultState(stateDefinition.any.setValue(ChameliumBlock.Color, DyeColor.BLACK))

  @Deprecated
  override def getDrops(state: BlockState, ctx: LootContext.Builder): List[ItemStack] = {
    val stack = new ItemStack(this, 1)
    stack.setDamageValue(state.getValue(ChameliumBlock.Color).getId)
    Collections.singletonList(stack)
  }
}
