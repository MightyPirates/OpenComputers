package li.cil.oc.util

import net.minecraft.block.Block

import scala.language.implicitConversions

object ExtendedBlock {

  implicit def extendedBlock(block: Block): ExtendedBlock = new ExtendedBlock(block)

  class ExtendedBlock(val block: Block) {
    def isAir(position: BlockPosition) = block.isAir(position.world.get, position.x, position.y, position.z)

    def isReplaceable(position: BlockPosition) = block.isReplaceable(position.world.get, position.x, position.y, position.z)
  }

}
