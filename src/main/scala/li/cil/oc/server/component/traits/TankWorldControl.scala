package li.cil.oc.server.component.traits

import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ExtendedBlock._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.block.BlockLiquid
import net.minecraftforge.fluids.FluidRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidBlock
import net.minecraftforge.fluids.IFluidHandler

trait TankWorldControl extends TankAware with WorldAware with SideRestricted {
  @Callback
  def compareFluid(context: Context, args: Arguments): Array[AnyRef] = {
    val side = checkSideForAction(args, 0)
    fluidInTank(selectedTank) match {
      case Some(stack) =>
        val blockPos = position.offset(side)
        if (world.blockExists(blockPos)) world.getTileEntity(blockPos) match {
          case handler: IFluidHandler =>
            result(Option(handler.getTankInfo(side.getOpposite)).exists(_.exists(other => stack.isFluidEqual(other.fluid))))
          case _ =>
            val block = world.getBlock(blockPos)
            val fluid = FluidRegistry.lookupFluidForBlock(block)
            result(stack.getFluid == fluid)
        }
        else result(false)
      case _ => result(false)
    }
  }

  @Callback
  def drain(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optionalFluidCount(1)
    getTank(selectedTank) match {
      case Some(tank) =>
        val space = tank.getCapacity - tank.getFluidAmount
        val amount = math.min(count, space)
        if (count > 0 && amount == 0) {
          result(Unit, "tank is full")
        }
        else {
          val blockPos = position.offset(facing)
          if (world.blockExists(blockPos)) world.getTileEntity(blockPos) match {
            case handler: IFluidHandler =>
              tank.getFluid match {
                case stack: FluidStack =>
                  val drained = handler.drain(facing.getOpposite, new FluidStack(stack, amount), true)
                  if ((drained != null && drained.amount > 0) || amount == 0) {
                    val filled = tank.fill(drained, true)
                    result(true, filled)
                  }
                  else result(Unit, "incompatible or no fluid")
                case _ =>
                  val transferred = tank.fill(handler.drain(facing.getOpposite, amount, true), true)
                  result(transferred > 0, transferred)
              }
            case _ => world.getBlock(blockPos) match {
              case fluidBlock: IFluidBlock if fluidBlock.canDrain(world, blockPos.toBlockPos) =>
                val drained = fluidBlock.drain(world, blockPos.toBlockPos, false)
                if ((drained != null && drained.amount > 0) && (drained.amount <= amount || amount == 0)) {
                  if (drained.amount <= amount) {
                    val filled = tank.fill(fluidBlock.drain(world, blockPos.toBlockPos, true), true)
                    result(true, filled)
                  }
                  else /* if (amount == 0) */ {
                    result(true, 0)
                  }
                }
                else result(Unit, "tank is full")
              case liquidBlock: BlockLiquid if world.getBlockState(blockPos.toBlockPos) == liquidBlock.getDefaultState =>
                val fluid = FluidRegistry.lookupFluidForBlock(liquidBlock)
                if (fluid == null) {
                  result(Unit, "incompatible or no fluid")
                }
                else if (tank.fill(new FluidStack(fluid, 1000), false) == 1000) {
                  tank.fill(new FluidStack(fluid, 1000), true)
                  world.setBlockToAir(blockPos)
                  result(true, 1000)
                }
                else result(Unit, "tank is full")
              case _ =>
                result(Unit, "incompatible or no fluid")
            }
          }
          else result(Unit, "incompatible or no fluid")
        }
      case _ => result(Unit, "no tank selected")
    }
  }

  @Callback
  def fill(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForAction(args, 0)
    val count = args.optionalFluidCount(1)
    getTank(selectedTank) match {
      case Some(tank) =>
        val amount = math.min(count, tank.getFluidAmount)
        if (count > 0 && amount == 0) {
          result(Unit, "tank is empty")
        }
        val blockPos = position.offset(facing)
        if (world.blockExists(blockPos)) world.getTileEntity(blockPos) match {
          case handler: IFluidHandler =>
            tank.getFluid match {
              case stack: FluidStack =>
                val filled = handler.fill(facing.getOpposite, new FluidStack(stack, amount), true)
                if (filled > 0 || amount == 0) {
                  tank.drain(filled, true)
                  result(true, filled)
                }
                else result(Unit, "incompatible or no fluid")
              case _ =>
                result(Unit, "tank is empty")
            }
          case _ =>
            val block = world.getBlock(blockPos)
            if (!block.isAir(blockPos) && !block.isReplaceable(blockPos)) {
              result(Unit, "no space")
            }
            else if (tank.getFluidAmount < 1000) {
              result(Unit, "tank is empty")
            }
            else if (!tank.getFluid.getFluid.canBePlacedInWorld) {
              result(Unit, "incompatible fluid")
            }
            else {
              val fluidBlock = tank.getFluid.getFluid.getBlock
              tank.drain(1000, true)
              world.breakBlock(blockPos)
              world.setBlock(blockPos, fluidBlock)
              // This fake neighbor update is required to get stills to start flowing.
              world.notifyBlockOfNeighborChange(blockPos, world.getBlock(position))
              result(true, 1000)
            }
        }
        else result(Unit, "no space")
      case _ => result(Unit, "no tank selected")
    }
  }
}
