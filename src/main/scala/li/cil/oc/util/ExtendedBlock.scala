package li.cil.oc.util

import net.minecraft.block.Block
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids.IFluidBlock

import scala.language.implicitConversions

object ExtendedBlock {

  implicit def extendedBlock(block: Block): ExtendedBlock = new ExtendedBlock(block)

  class ExtendedBlock(val block: Block) {
    def isAir(position: BlockPosition) = block.isAir(position.world.get.getBlockState(position.toBlockPos), position.world.get, position.toBlockPos)

    def isReplaceable(position: BlockPosition) = block.isReplaceable(position.world.get, position.toBlockPos)

    def getBlockHardness(position: BlockPosition) = block.getBlockHardness(position.world.get.getBlockState(position.toBlockPos), position.world.get, position.toBlockPos)

    def getSelectedBoundingBoxFromPool(position: BlockPosition) = block.getSelectedBoundingBox(position.world.get.getBlockState(position.toBlockPos), position.world.get, position.toBlockPos)

    def getCollisionBoundingBoxFromPool(position: BlockPosition) = block.getCollisionBoundingBox(position.world.get.getBlockState(position.toBlockPos), position.world.get, position.toBlockPos)

    def getComparatorInputOverride(position: BlockPosition, side: EnumFacing) = block.getComparatorInputOverride(position.world.get.getBlockState(position.toBlockPos), position.world.get, position.toBlockPos)
  }

  implicit def extendedFluidBlock(block: IFluidBlock): ExtendedFluidBlock = new ExtendedFluidBlock(block)

  class ExtendedFluidBlock(val block: IFluidBlock) {
    def drain(position: BlockPosition, doDrain: Boolean) = block.drain(position.world.get, position.toBlockPos, doDrain)

    def canDrain(position: BlockPosition) = block.canDrain(position.world.get, position.toBlockPos)

    def getFilledPercentage(position: BlockPosition) = block.getFilledPercentage(position.world.get, position.toBlockPos)
  }

}
