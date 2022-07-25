package li.cil.oc.util

import li.cil.oc.util.ExtendedBlock._
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraft.block.FlowingFluidBlock
import net.minecraft.block.Blocks
import net.minecraft.fluid.Fluid
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraftforge.fluids.FluidAttributes
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidBlock
import net.minecraftforge.fluids.capability.CapabilityFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction
import net.minecraftforge.fluids.capability.IFluidHandlerItem

object FluidUtils {
  /**
   * Retrieves an actual fluid handler implementation for a specified world coordinate.
   * <p/>
   * This performs special handling for in-world liquids.
   */
  def fluidHandlerAt(position: BlockPosition, side: Direction): Option[IFluidHandler] = position.world match {
    case Some(world) if world.blockExists(position) => world.getBlockEntity(position) match {
      case handler: IFluidHandler => Option(handler)
      case t: TileEntity if t.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side).isPresent =>
        t.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side).orElse(null) match {
          case handler: IFluidHandler => Option(handler)
          case _ => Option(new GenericBlockWrapper(position))
        }
      case _ => Option(new GenericBlockWrapper(position))
    }
    case _ => None
  }

  def fluidHandlerOf(stack: ItemStack): IFluidHandlerItem = Option(stack) match {
    case Some(itemStack) => itemStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null).orElse(null)
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
  def transferBetweenFluidHandlers(source: IFluidHandler, sink: IFluidHandler, limit: Int = FluidAttributes.BUCKET_VOLUME): Int = {
    val drained = source.drain(limit, FluidAction.SIMULATE)
    if (drained == null) {
      return 0
    }
    val filled = sink.fill(drained, FluidAction.SIMULATE)
    sink.fill(source.drain(filled, FluidAction.EXECUTE), FluidAction.EXECUTE)
  }

  /**
   * Utility method for calling <tt>transferBetweenFluidHandlers</tt> on handlers
   * in the world.
   * <p/>
   * This uses the <tt>fluidHandlerAt</tt> method, and therefore handles special
   * cases such as fluid blocks.
   */
  def transferBetweenFluidHandlersAt(sourcePos: BlockPosition, sourceSide: Direction, sinkPos: BlockPosition, sinkSide: Direction, limit: Int = FluidAttributes.BUCKET_VOLUME): Int =
    fluidHandlerAt(sourcePos, sourceSide).fold(0)(source =>
      fluidHandlerAt(sinkPos, sinkSide).fold(0)(sink =>
        transferBetweenFluidHandlers(source, sink, limit)))

  /**
   * Lookup fluid taking into account flowing liquid blocks...
   * For legacy reasons, returns null when the block is not a fluid, not Fluids.EMPTY.
   */
  @Deprecated
  def lookupFluidForBlock(block: Block): Fluid = block match {
    case fluid: FlowingFluidBlock => fluid.getFluid
    case _ => null
  }

  // ----------------------------------------------------------------------- //

  private class GenericBlockWrapper(position: BlockPosition) extends IFluidHandler {
    override def getTanks = currentWrapper.fold(0)(_.getTanks)

    override def getFluidInTank(tank: Int) = currentWrapper.fold(FluidStack.EMPTY)(_.getFluidInTank(tank))

    override def getTankCapacity(tank: Int) = currentWrapper.fold(0)(_.getTankCapacity(tank))

    override def isFluidValid(tank: Int, fluid: FluidStack): Boolean = currentWrapper.fold(false)(_.isFluidValid(tank, fluid))

    override def drain(resource: FluidStack, action: FluidAction): FluidStack = currentWrapper.fold(null: FluidStack)(_.drain(resource, action))

    override def drain(maxDrain: Int, action: FluidAction): FluidStack = currentWrapper.fold(null: FluidStack)(_.drain(maxDrain, action))

    override def fill(resource: FluidStack, action: FluidAction): Int = currentWrapper.fold(0)(_.fill(resource, action))

    def currentWrapper: Option[IFluidHandler] = if (position.world.get.blockExists(position)) position.world.get.getBlock(position) match {
      case block: IFluidBlock => Option(new FluidBlockWrapper(position, block))
      case block: FlowingFluidBlock if lookupFluidForBlock(block) != null && isFullLiquidBlock => Option(new LiquidBlockWrapper(position, block))
      case block: Block if block.isAir(position) || block.isReplaceable(position) => Option(new AirBlockWrapper(position, block))
      case _ => None
    }
    else None

    def isFullLiquidBlock: Boolean = {
      val state = position.world.get.getBlockState(position.toBlockPos)
      state.getValue(FlowingFluidBlock.LEVEL) == 0
    }
  }

  private trait BlockWrapperBase extends IFluidHandler {
    override def getTanks = 1

    override def getTankCapacity(tank: Int) = FluidAttributes.BUCKET_VOLUME

    protected def uncheckedDrain(action: FluidAction): FluidStack

    override def drain(resource: FluidStack, action: FluidAction): FluidStack = {
      val drained = uncheckedDrain(FluidAction.SIMULATE)
      if (drained != null && (resource == null || (drained.getFluid == resource.getFluid && drained.getAmount <= resource.getAmount))) {
        uncheckedDrain(action)
      }
      else null
    }

    override def drain(maxDrain: Int, action: FluidAction): FluidStack = {
      val drained = uncheckedDrain(FluidAction.SIMULATE)
      if (drained != null && drained.getAmount <= maxDrain) {
        uncheckedDrain(action)
      }
      else null
    }

    override def fill(resource: FluidStack, action: FluidAction): Int = 0
  }

  @Deprecated
  private class FluidBlockWrapper(val position: BlockPosition, val block: IFluidBlock) extends BlockWrapperBase {
    override def getFluidInTank(tank: Int) = block.drain(position, FluidAction.SIMULATE)

    override def isFluidValid(tank: Int, fluid: FluidStack): Boolean = block.getFluid.isSame(fluid.getFluid) && block.canDrain(position)

    override protected def uncheckedDrain(action: FluidAction): FluidStack = block.drain(position, action)
  }

  private class LiquidBlockWrapper(val position: BlockPosition, val block: FlowingFluidBlock) extends BlockWrapperBase {
    val fluid: Fluid = lookupFluidForBlock(block)

    override def getFluidInTank(tank: Int) = if (isFullLiquidBlock) new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME) else FluidStack.EMPTY

    override def isFluidValid(tank: Int, fluid: FluidStack): Boolean = block.getFluid.isSame(fluid.getFluid)

    override protected def uncheckedDrain(action: FluidAction): FluidStack = {
      if (action.execute) {
        position.world.get.setBlockToAir(position)
      }
      if (isFullLiquidBlock) new FluidStack(fluid, FluidAttributes.BUCKET_VOLUME) else FluidStack.EMPTY
    }

    def isFullLiquidBlock: Boolean = {
      val state = position.world.get.getBlockState(position.toBlockPos)
      state.getValue(FlowingFluidBlock.LEVEL) == 0
    }
  }

  private class AirBlockWrapper(val position: BlockPosition, val block: Block) extends IFluidHandler {
    override def getTanks = 1

    override def getTankCapacity(tank: Int) = FluidAttributes.BUCKET_VOLUME

    override def getFluidInTank(tank: Int) = FluidStack.EMPTY

    override def drain(resource: FluidStack, action: FluidAction): FluidStack = FluidStack.EMPTY

    override def drain(maxDrain: Int, action: FluidAction): FluidStack = FluidStack.EMPTY

    override def isFluidValid(tank: Int, fluid: FluidStack): Boolean = fluid.getFluid.defaultFluidState.createLegacyBlock != null

    override def fill(resource: FluidStack, action: FluidAction): Int = {
      if (resource != null && resource.getFluid.defaultFluidState.createLegacyBlock != null && resource.getAmount >= FluidAttributes.BUCKET_VOLUME) {
        if (action.execute) {
          val world = position.world.get
          if (!world.isAirBlock(position) && !world.containsAnyLiquid(position.bounds))
            world.breakBlock(position)
          world.setBlockAndUpdate(position.toBlockPos, resource.getFluid.defaultFluidState.createLegacyBlock)
          // This fake neighbor update is required to get stills to start flowing.
          world.notifyBlockOfNeighborChange(position, world.getBlock(position))
        }
        FluidAttributes.BUCKET_VOLUME
      }
      else 0
    }
  }

}
