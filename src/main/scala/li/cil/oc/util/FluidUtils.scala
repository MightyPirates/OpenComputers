package li.cil.oc.util

import li.cil.oc.util.ExtendedBlock._
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraft.block.BlockLiquid
import net.minecraft.block.BlockStaticLiquid
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidContainerRegistry
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.FluidTank
import net.minecraftforge.fluids.FluidTankInfo
import net.minecraftforge.fluids.IFluidBlock
import net.minecraftforge.fluids.IFluidHandler

object FluidUtils {
  /**
   * Retrieves an actual fluid handler implementation for a specified world coordinate.
   * <p/>
   * This performs special handling for in-world liquids.
   */
  def fluidHandlerAt(position: BlockPosition): Option[IFluidHandler] = position.world match {
    case Some(world) if world.blockExists(position) => world.getTileEntity(position) match {
      case handler: IFluidHandler => Option(handler)
      case _ => Option(new GenericBlockWrapper(position))
    }
    case _ => None
  }

  /**
   * Transfers some fluid between two fluid handlers.
   * <p/>
   * This will try to extract up the specified amount of fluid from any handler,
   * then insert it into the specified sink handler. If the insertion fails, the
   * fluid will remain in the source handler.
   * <p/>
   * This returns <tt>true</tt> if some fluid was transferred.
   */
  def transferBetweenFluidHandlers(source: IFluidHandler, sourceSide: EnumFacing, sink: IFluidHandler, sinkSide: EnumFacing, limit: Int = FluidContainerRegistry.BUCKET_VOLUME) = {
    val drained = source.drain(sourceSide, limit, false)
    val filled = sink.fill(sinkSide, drained, false)
    sink.fill(sinkSide, source.drain(sourceSide, filled, true), true)
  }

  /**
   * Utility method for calling <tt>transferBetweenFluidHandlers</tt> on handlers
   * in the world.
   * <p/>
   * This uses the <tt>fluidHandlerAt</tt> method, and therefore handles special
   * cases such as fluid blocks.
   */
  def transferBetweenFluidHandlersAt(sourcePos: BlockPosition, sourceSide: EnumFacing, sinkPos: BlockPosition, sinkSide: EnumFacing, limit: Int = FluidContainerRegistry.BUCKET_VOLUME) =
    fluidHandlerAt(sourcePos).fold(0)(source =>
      fluidHandlerAt(sinkPos).fold(0)(sink =>
        transferBetweenFluidHandlers(source, sourceSide, sink, sinkSide, limit)))

  // ----------------------------------------------------------------------- //

  private class GenericBlockWrapper(position: BlockPosition) extends IFluidHandler {
    override def canDrain(from: EnumFacing, fluid: Fluid): Boolean = currentWrapper.fold(false)(_.canDrain(from, fluid))

    override def drain(from: EnumFacing, resource: FluidStack, doDrain: Boolean): FluidStack = currentWrapper.fold(null: FluidStack)(_.drain(from, resource, doDrain))

    override def drain(from: EnumFacing, maxDrain: Int, doDrain: Boolean): FluidStack = currentWrapper.fold(null: FluidStack)(_.drain(from, maxDrain, doDrain))

    override def canFill(from: EnumFacing, fluid: Fluid): Boolean = currentWrapper.fold(false)(_.canFill(from, fluid))

    override def fill(from: EnumFacing, resource: FluidStack, doFill: Boolean): Int = currentWrapper.fold(0)(_.fill(from, resource, doFill))

    override def getTankInfo(from: EnumFacing): Array[FluidTankInfo] = currentWrapper.fold(Array.empty[FluidTankInfo])(_.getTankInfo(from))

    def currentWrapper = if (position.world.get.blockExists(position)) position.world.get.getBlock(position) match {
      case block: IFluidBlock => Option(new FluidBlockWrapper(position, block))
      case block: BlockStaticLiquid if FluidRegistry.lookupFluidForBlock(block) != null && isFullLiquidBlock => Option(new LiquidBlockWrapper(position, block))
      case block: Block if block.isAir(position) || block.isReplaceable(position) => Option(new AirBlockWrapper(position, block))
      case _ => None
    }
    else None

    def isFullLiquidBlock = {
      val state = position.world.get.getBlockState(position.toBlockPos)
      state.getValue(BlockLiquid.LEVEL) == 0
    }
  }

  private trait BlockWrapperBase extends IFluidHandler {
    protected def uncheckedDrain(doDrain: Boolean): FluidStack

    override def drain(from: EnumFacing, resource: FluidStack, doDrain: Boolean): FluidStack = {
      val drained = uncheckedDrain(false)
      if (drained != null && (resource == null || (drained.getFluid == resource.getFluid && drained.amount <= resource.amount))) {
        uncheckedDrain(doDrain)
      }
      else null
    }

    override def drain(from: EnumFacing, maxDrain: Int, doDrain: Boolean): FluidStack = {
      val drained = uncheckedDrain(false)
      if (drained != null && drained.amount <= maxDrain) {
        uncheckedDrain(doDrain)
      }
      else null
    }

    override def canFill(from: EnumFacing, fluid: Fluid): Boolean = false

    override def fill(from: EnumFacing, resource: FluidStack, doFill: Boolean): Int = 0
  }

  private class FluidBlockWrapper(val position: BlockPosition, val block: IFluidBlock) extends BlockWrapperBase {
    final val AssumedCapacity = FluidContainerRegistry.BUCKET_VOLUME

    override def canDrain(from: EnumFacing, fluid: Fluid): Boolean = block.canDrain(position)

    override def getTankInfo(from: EnumFacing): Array[FluidTankInfo] = Array(new FluidTankInfo(new FluidTank(block.getFluid, (block.getFilledPercentage(position) * AssumedCapacity).toInt, AssumedCapacity)))

    override protected def uncheckedDrain(doDrain: Boolean): FluidStack = block.drain(position, doDrain)
  }

  private class LiquidBlockWrapper(val position: BlockPosition, val block: BlockLiquid) extends BlockWrapperBase {
    val fluid = FluidRegistry.lookupFluidForBlock(block)

    override def canDrain(from: EnumFacing, fluid: Fluid): Boolean = true

    override def getTankInfo(from: EnumFacing): Array[FluidTankInfo] = Array(new FluidTankInfo(new FluidTank(fluid, FluidContainerRegistry.BUCKET_VOLUME, FluidContainerRegistry.BUCKET_VOLUME)))

    override protected def uncheckedDrain(doDrain: Boolean): FluidStack = {
      if (doDrain) {
        position.world.get.setBlockToAir(position)
      }
      new FluidStack(fluid, FluidContainerRegistry.BUCKET_VOLUME)
    }
  }

  private class AirBlockWrapper(val position: BlockPosition, val block: Block) extends IFluidHandler {
    override def canDrain(from: EnumFacing, fluid: Fluid): Boolean = false

    override def drain(from: EnumFacing, resource: FluidStack, doDrain: Boolean): FluidStack = null

    override def drain(from: EnumFacing, maxDrain: Int, doDrain: Boolean): FluidStack = null

    override def canFill(from: EnumFacing, fluid: Fluid): Boolean = fluid.canBePlacedInWorld

    override def fill(from: EnumFacing, resource: FluidStack, doFill: Boolean): Int = {
      if (resource != null && resource.getFluid.canBePlacedInWorld && resource.getFluid.getBlock != null) {
        if (doFill) {
          val world = position.world.get
          if (!world.isAirBlock(position) && !world.isAnyLiquid(position.bounds))
            world.breakBlock(position)
          world.setBlock(position, resource.getFluid.getBlock)
          // This fake neighbor update is required to get stills to start flowing.
          world.notifyBlockOfNeighborChange(position, world.getBlock(position))
        }
        FluidContainerRegistry.BUCKET_VOLUME
      }
      else 0
    }

    override def getTankInfo(from: EnumFacing): Array[FluidTankInfo] = Array.empty
  }

}
