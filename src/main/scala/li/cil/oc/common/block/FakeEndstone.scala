package li.cil.oc.common.block

import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState

class FakeEndstone extends SimpleBlock(Material.ROCK) {
  setHardness(3)
  setResistance(15)

  override def hasTileEntity(state: IBlockState): Boolean = false
}
