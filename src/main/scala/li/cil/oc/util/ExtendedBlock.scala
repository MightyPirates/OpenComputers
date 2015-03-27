package li.cil.oc.util

import net.minecraft.block.Block

import scala.language.implicitConversions

object ExtendedBlock {

  implicit def extendedBlock(block: Block): ExtendedBlock = new ExtendedBlock(block)

  class ExtendedBlock(val block: Block) {
    def isAir(position: BlockPosition) = block.isAir(position.world.get, position.toBlockPos)

    def isReplaceable(position: BlockPosition) = block.isReplaceable(position.world.get, position.toBlockPos)

    def getBlockHardness(position: BlockPosition) = block.getBlockHardness(position.world.get, position.toBlockPos)

    def setBlockBoundsBasedOnState(position: BlockPosition) = block.setBlockBoundsBasedOnState(position.world.get, position.toBlockPos)

    def getSelectedBoundingBoxFromPool(position: BlockPosition) = block.getSelectedBoundingBox(position.world.get, position.toBlockPos)

    def getCollisionBoundingBoxFromPool(position: BlockPosition) = block.getCollisionBoundingBox(position.world.get, position.toBlockPos, position.world.get.getBlockState(position.toBlockPos))
  }

}
