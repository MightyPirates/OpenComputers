package li.cil.oc.util

import net.minecraft.block.Block
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.IFluidBlock

import scala.language.implicitConversions

object ExtendedBlock {

  implicit def extendedBlock(block: Block): ExtendedBlock = new ExtendedBlock(block)

  class ExtendedBlock(val block: Block) {
    def isAir(position: BlockPosition) = block.isAir(position.world.get, position.x, position.y, position.z)

    def isReplaceable(position: BlockPosition) = block.isReplaceable(position.world.get, position.x, position.y, position.z)

    def setBlockBoundsBasedOnState(position: BlockPosition) = block.setBlockBoundsBasedOnState(position.world.get, position.x, position.y, position.z)

    def getSelectedBoundingBoxFromPool(position: BlockPosition) = block.getSelectedBoundingBoxFromPool(position.world.get, position.x, position.y, position.z)

    def getCollisionBoundingBoxFromPool(position: BlockPosition) = block.getCollisionBoundingBoxFromPool(position.world.get, position.x, position.y, position.z)

    def getComparatorInputOverride(position: BlockPosition, side: ForgeDirection) = block.getComparatorInputOverride(position.world.get, position.x, position.y, position.z, side.ordinal())
  }

  implicit def extendedFluidBlock(block: IFluidBlock): ExtendedFluidBlock = new ExtendedFluidBlock(block)

  class ExtendedFluidBlock(val block: IFluidBlock) {
    def drain(position: BlockPosition, doDrain: Boolean) = block.drain(position.world.get, position.x, position.y, position.z, doDrain)

    def canDrain(position: BlockPosition) = block.canDrain(position.world.get, position.x, position.y, position.z)

    def getFilledPercentage(position: BlockPosition) = block.getFilledPercentage(position.world.get, position.x, position.y, position.z)
  }

}
