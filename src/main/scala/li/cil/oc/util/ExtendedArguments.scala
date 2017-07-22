package li.cil.oc.util

import li.cil.oc.api.internal.MultiTank
import li.cil.oc.api.machine.Arguments
import net.minecraft.inventory.IInventory
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.FluidContainerRegistry
import net.minecraftforge.fluids.FluidTankInfo
import net.minecraftforge.fluids.IFluidHandler

import scala.language.implicitConversions

object ExtendedArguments {

  implicit def extendedArguments(args: Arguments): ExtendedArguments = new ExtendedArguments(args)

  class ExtendedArguments(val args: Arguments) {
    def optItemCount(index: Int, default: Int = 64) =
      if (!isDefined(index) || !hasValue(index)) default
      else math.max(0, math.min(64, args.checkInteger(index)))

    def optFluidCount(index: Int, default: Int = FluidContainerRegistry.BUCKET_VOLUME) =
      if (!isDefined(index) || !hasValue(index)) default
      else math.max(0, args.checkInteger(index))

    def checkSlot(inventory: IInventory, n: Int) = {
      val slot = args.checkInteger(n) - 1
      if (slot < 0 || slot >= inventory.getSizeInventory) {
        throw new IllegalArgumentException("invalid slot")
      }
      slot
    }

    def optSlot(inventory: IInventory, index: Int, default: Int) = {
      if (!isDefined(index)) default
      else checkSlot(inventory, index)
    }

    def checkTank(multi: MultiTank, n: Int) = {
      val tank = args.checkInteger(n) - 1
      if (tank < 0 || tank >= multi.tankCount) {
        throw new IllegalArgumentException("invalid tank index")
      }
      tank
    }

    def checkTankInfo(handler: IFluidHandler, side: ForgeDirection, n: Int) = {
      val tank = args.checkInteger(n) - 1
      if (tank < 0 || tank >= handler.getTankInfo(side).length) {
        throw new IllegalArgumentException("invalid tank index")
      }
      handler.getTankInfo(side)(tank)
    }

    def optTankInfo(handler: IFluidHandler, side: ForgeDirection, n: Int, default: FluidTankInfo) = {
      if (!isDefined(n)) default
      else checkTankInfo(handler, side, n)
    }

    def checkSideAny(index: Int) = checkSide(index, ForgeDirection.VALID_DIRECTIONS: _*)

    def optSideAny(index: Int, default: ForgeDirection) =
      if (!isDefined(index)) default
      else checkSideAny(index)

    def checkSideExcept(index: Int, invalid: ForgeDirection*) = checkSide(index, ForgeDirection.VALID_DIRECTIONS.filterNot(invalid.contains): _*)

    def optSideExcept(index: Int, default: ForgeDirection, invalid: ForgeDirection*) =
      if (!isDefined(index)) default
      else checkSideExcept(index, invalid: _*)

    def checkSideForAction(index: Int) = checkSide(index, ForgeDirection.SOUTH, ForgeDirection.UP, ForgeDirection.DOWN)

    def optSideForAction(index: Int, default: ForgeDirection) =
      if (!isDefined(index)) default
      else checkSideForAction(index)

    def checkSideForMovement(index: Int) = checkSide(index, ForgeDirection.SOUTH, ForgeDirection.NORTH, ForgeDirection.UP, ForgeDirection.DOWN)

    def optSideForMovement(index: Int, default: ForgeDirection) =
      if (!isDefined(index)) default
      else checkSideForMovement(index)

    def checkSideForFace(index: Int, facing: ForgeDirection) = checkSideExcept(index, facing.getOpposite)

    def optSideForFace(index: Int, default: ForgeDirection) =
      if (!isDefined(index)) default
      else checkSideForAction(index)

    private def checkSide(index: Int, allowed: ForgeDirection*) = {
      val side = args.checkInteger(index)
      if (side < 0 || side > 5) {
        throw new IllegalArgumentException("invalid side")
      }
      val direction = ForgeDirection.getOrientation(side)
      if (allowed.isEmpty || (allowed contains direction)) direction
      else throw new IllegalArgumentException("unsupported side")
    }

    private def isDefined(index: Int) = index >= 0 && index < args.count()

    private def hasValue(index: Int) = args.checkAny(index) != null
  }

}
