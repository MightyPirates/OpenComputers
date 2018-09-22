package li.cil.oc.util

import li.cil.oc.util.ExtendedBlock._
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraft.block.BlockDynamicLiquid
import net.minecraft.block.BlockLiquid
import net.minecraft.block.BlockStaticLiquid
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidBlock
import net.minecraftforge.fluids.capability
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.FluidTankProperties
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandlerItem
import net.minecraftforge.fluids.capability.IFluidTankProperties

object FluidUtils {
  /**
   * Retrieves an actual fluid handler implementation for a specified world coordinate.
   * <p/>
   * This performs special handling for in-world liquids.
   */
  def fluidHandlerAt(position: BlockPosition, side: EnumFacing): Option[IFluidHandler] = position.world match {
    case Some(world) if world.blockExists(position) => world.getTileEntity(position) match {
      case handler: IFluidHandler => Option(handler)
      case t: TileEntity if t.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side) =>
        t.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side) match {
          case handler: capability.IFluidHandler => Option(handler)
          case _ => Option(new GenericBlockWrapper(position))
        }
      case _ => Option(new GenericBlockWrapper(position))
    }
    case _ => None
  }

  def fluidHandlerOf(stack: ItemStack): IFluidHandlerItem = Option(stack) match {
    case Some(itemStack) if itemStack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null) =>
      itemStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null)
    case _ => null
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
  def transferBetweenFluidHandlers(source: IFluidHandler, sink: IFluidHandler, limit: Int = Fluid.BUCKET_VOLUME): Int = {
    val drained = source.drain(limit, false)
    if (drained == null) {
      return 0
    }
    val filled = sink.fill(drained, false)
    sink.fill(source.drain(filled, true), true)
  }

  /**
   * Utility method for calling <tt>transferBetweenFluidHandlers</tt> on handlers
   * in the world.
   * <p/>
   * This uses the <tt>fluidHandlerAt</tt> method, and therefore handles special
   * cases such as fluid blocks.
   */
  def transferBetweenFluidHandlersAt(sourcePos: BlockPosition, sourceSide: EnumFacing, sinkPos: BlockPosition, sinkSide: EnumFacing, limit: Int = Fluid.BUCKET_VOLUME): Int =
    fluidHandlerAt(sourcePos, sourceSide).fold(0)(source =>
      fluidHandlerAt(sinkPos, sinkSide).fold(0)(sink =>
        transferBetweenFluidHandlers(source, sink, limit)))

  /**
   * Lookup fluid taking into account flowing liquid blocks...
   */
  def lookupFluidForBlock(block: Block): Fluid = {
    if (block == Blocks.FLOWING_LAVA) FluidRegistry.LAVA
    else if (block == Blocks.FLOWING_WATER) FluidRegistry.WATER
    else FluidRegistry.lookupFluidForBlock(block)
  }

  // ----------------------------------------------------------------------- //

  private class GenericBlockWrapper(position: BlockPosition) extends IFluidHandler {
    def canDrain(fluid: Fluid): Boolean = currentWrapper.fold(false)(_.drain(new FluidStack(fluid, 1), false).amount > 0)

    override def drain(resource: FluidStack, doDrain: Boolean): FluidStack = currentWrapper.fold(null: FluidStack)(_.drain(resource, doDrain))

    override def drain(maxDrain: Int, doDrain: Boolean): FluidStack = currentWrapper.fold(null: FluidStack)(_.drain(maxDrain, doDrain))

    def canFill(fluid: Fluid): Boolean = currentWrapper.fold(false)(_.fill(new FluidStack(fluid, 1), false) > 0)

    override def fill(resource: FluidStack, doFill: Boolean): Int = currentWrapper.fold(0)(_.fill(resource, doFill))

    override def getTankProperties: Array[IFluidTankProperties] = currentWrapper.fold(Array.empty[IFluidTankProperties])(_.getTankProperties)

    def currentWrapper: Option[IFluidHandler] = if (position.world.get.blockExists(position)) position.world.get.getBlock(position) match {
      case block: IFluidBlock => Option(new FluidBlockWrapper(position, block))
      case block: BlockStaticLiquid if lookupFluidForBlock(block) != null && isFullLiquidBlock => Option(new LiquidBlockWrapper(position, block))
      case block: BlockDynamicLiquid if lookupFluidForBlock(block) != null && isFullLiquidBlock => Option(new LiquidBlockWrapper(position, block))
      case block: Block if block.isAir(position) || block.isReplaceable(position) => Option(new AirBlockWrapper(position, block))
      case _ => None
    }
    else None

    def isFullLiquidBlock: Boolean = {
      val state = position.world.get.getBlockState(position.toBlockPos)
      state.getValue(BlockLiquid.LEVEL) == 0
    }
  }

  private trait BlockWrapperBase extends IFluidHandler {
    protected def uncheckedDrain(doDrain: Boolean): FluidStack

    override def drain(resource: FluidStack, doDrain: Boolean): FluidStack = {
      val drained = uncheckedDrain(false)
      if (drained != null && (resource == null || (drained.getFluid == resource.getFluid && drained.amount <= resource.amount))) {
        uncheckedDrain(doDrain)
      }
      else null
    }

    override def drain(maxDrain: Int, doDrain: Boolean): FluidStack = {
      val drained = uncheckedDrain(false)
      if (drained != null && drained.amount <= maxDrain) {
        uncheckedDrain(doDrain)
      }
      else null
    }

    def canFill(fluid: Fluid): Boolean = false

    override def fill(resource: FluidStack, doFill: Boolean): Int = 0
  }

  private class FluidBlockWrapper(val position: BlockPosition, val block: IFluidBlock) extends BlockWrapperBase {
    final val AssumedCapacity = Fluid.BUCKET_VOLUME

    def canDrain(fluid: Fluid): Boolean = block.canDrain(position)

    override def getTankProperties: Array[IFluidTankProperties] = Array(new FluidTankProperties(new FluidStack(block.getFluid, (block.getFilledPercentage(position) * AssumedCapacity).toInt), AssumedCapacity))

    override protected def uncheckedDrain(doDrain: Boolean): FluidStack = block.drain(position, doDrain)
  }

  private class LiquidBlockWrapper(val position: BlockPosition, val block: BlockLiquid) extends BlockWrapperBase {
    val fluid: Fluid = lookupFluidForBlock(block)

    def canDrain(fluid: Fluid): Boolean = true

    override def getTankProperties: Array[IFluidTankProperties] = Array(new FluidTankProperties(new FluidStack(fluid, Fluid.BUCKET_VOLUME), Fluid.BUCKET_VOLUME))

    override protected def uncheckedDrain(doDrain: Boolean): FluidStack = {
      if (doDrain) {
        position.world.get.setBlockToAir(position)
      }
      new FluidStack(fluid, Fluid.BUCKET_VOLUME)
    }
  }

  private class AirBlockWrapper(val position: BlockPosition, val block: Block) extends IFluidHandler {
    def canDrain(fluid: Fluid): Boolean = false

    override def drain(resource: FluidStack, doDrain: Boolean): FluidStack = null

    override def drain(maxDrain: Int, doDrain: Boolean): FluidStack = null

    def canFill(fluid: Fluid): Boolean = fluid.canBePlacedInWorld

    override def fill(resource: FluidStack, doFill: Boolean): Int = {
      if (resource != null && resource.getFluid.canBePlacedInWorld && resource.getFluid.getBlock != null) {
        if (doFill) {
          val world = position.world.get
          if (!world.isAirBlock(position) && !world.containsAnyLiquid(position.bounds))
            world.breakBlock(position)
          world.setBlock(position, resource.getFluid.getBlock)
          // This fake neighbor update is required to get stills to start flowing.
          world.notifyBlockOfNeighborChange(position, world.getBlock(position))
        }
        Fluid.BUCKET_VOLUME
      }
      else 0
    }

    override def getTankProperties: Array[IFluidTankProperties] = Array.empty
  }

}
