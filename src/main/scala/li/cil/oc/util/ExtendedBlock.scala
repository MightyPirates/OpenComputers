package li.cil.oc.util

import net.minecraft.block.Block
import net.minecraft.util.Direction
import net.minecraftforge.fluids.IFluidBlock
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction

import scala.language.implicitConversions

object ExtendedBlock {

  implicit def extendedBlock(block: Block): ExtendedBlock = new ExtendedBlock(block)

  class ExtendedBlock(val block: Block) {
    @Deprecated
    def isAir(position: BlockPosition) = block.isAir(position.world.get.getBlockState(position.toBlockPos), position.world.get, position.toBlockPos)

    @Deprecated
    def isReplaceable(position: BlockPosition) = block.defaultBlockState.getMaterial.isReplaceable

    @Deprecated
    def getBlockHardness(position: BlockPosition) = position.world.get.getBlockState(position.toBlockPos).getDestroySpeed(position.world.get, position.toBlockPos)

    @Deprecated
    def getComparatorInputOverride(position: BlockPosition, side: Direction) = block.getAnalogOutputSignal(position.world.get.getBlockState(position.toBlockPos), position.world.get, position.toBlockPos)
  }

  implicit def extendedFluidBlock(block: IFluidBlock): ExtendedFluidBlock = new ExtendedFluidBlock(block)

  class ExtendedFluidBlock(val block: IFluidBlock) {
    def drain(position: BlockPosition, action: FluidAction) = block.drain(position.world.get, position.toBlockPos, action)

    def canDrain(position: BlockPosition) = block.canDrain(position.world.get, position.toBlockPos)

    def getFilledPercentage(position: BlockPosition) = block.getFilledPercentage(position.world.get, position.toBlockPos)
  }

}
