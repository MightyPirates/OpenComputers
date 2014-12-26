package li.cil.oc.util

import net.minecraft.block.Block
import net.minecraft.world.World

import scala.language.implicitConversions

object ExtendedBlock {

  implicit def extendedBlock(block: Block): ExtendedBlock = new ExtendedBlock(block)

  class ExtendedBlock(val block: Block) {
    def isAir(position: BlockPosition) = block.isAir(position.world.get, position.toBlockPos)

    def isReplaceable(position: BlockPosition) = block.isReplaceable(position.world.get, position.toBlockPos)

    def getBlockHardness(position: BlockPosition) = block.getBlockHardness(position.world.get, position.toBlockPos)
  }

}
