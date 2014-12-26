package li.cil.oc.common.block.traits

import li.cil.oc.common.block.SimpleBlock
import net.minecraft.util.BlockPos
import net.minecraft.world.IBlockAccess

trait SpecialBlock extends SimpleBlock {
  override def isNormalCube(world: IBlockAccess, pos: BlockPos) = false

  override def isOpaqueCube = false

  // TODO new equivalent?
//  override def renderAsNormalBlock = false
}
