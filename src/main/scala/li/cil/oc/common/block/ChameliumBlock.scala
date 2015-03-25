package li.cil.oc.common.block

import li.cil.oc.util.Color
import net.minecraft.block.state.IBlockState

class ChameliumBlock extends SimpleBlock {
  override def getRenderColor(state: IBlockState): Int = Color.byOreName(Color.dyes(getMetaFromState(state) max 0 min Color.dyes.length))

  override def damageDropped(state: IBlockState): Int = getMetaFromState(state)
}
