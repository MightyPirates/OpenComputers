package li.cil.oc.util

import li.cil.oc.api.internal.MultiTank
import li.cil.oc.api.machine.Arguments
import net.minecraft.inventory.IInventory
import net.minecraft.util.Direction
import net.minecraftforge.fluids.FluidAttributes
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.items.IItemHandler

import scala.language.implicitConversions

object ExtendedArguments {

  implicit def extendedArguments(args: Arguments): ExtendedArguments = new ExtendedArguments(args)

  class ExtendedArguments(val args: Arguments) {
    def optItemCount(index: Int, default: Int = 64) =
      if (!isDefined(index) || !hasValue(index)) default
      else math.max(0, math.min(64, args.checkInteger(index)))

    def optFluidCount(index: Int, default: Int = FluidAttributes.BUCKET_VOLUME) =
      if (!isDefined(index) || !hasValue(index)) default
      else math.max(0, args.checkInteger(index))

    def checkSlot(inventory: IItemHandler, n: Int): Int = {
      val slot = args.checkInteger(n) - 1
      if (slot < 0 || slot >= inventory.getSlots) {
        throw new IllegalArgumentException("invalid slot")
      }
      slot
    }

    def optSlot(inventory: IItemHandler, index: Int, default: Int): Int = {
      if (!isDefined(index)) default
      else checkSlot(inventory, index)
    }

    def checkSlot(inventory: IInventory, n: Int): Int = checkSlot(InventoryUtils.asItemHandler(inventory), n)

    def optSlot(inventory: IInventory, index: Int, default: Int): Int = optSlot(InventoryUtils.asItemHandler(inventory), index, default)

    def checkTank(multi: MultiTank, n: Int) = {
      val tank = args.checkInteger(n) - 1
      if (tank < 0 || tank >= multi.tankCount) {
        throw new IllegalArgumentException("invalid tank index")
      }
      tank
    }

    def checkTankProperties(handler: IFluidHandler, n: Int) = {
      val tank = args.checkInteger(n) - 1
      if (tank < 0 || tank >= handler.getTanks) {
        throw new IllegalArgumentException("invalid tank index")
      }
      new TankProperties(handler.getTankCapacity(tank), handler.getFluidInTank(tank))
    }

    def optTankProperties(handler: IFluidHandler, n: Int, default: TankProperties) = {
      if (!isDefined(n)) default
      else checkTankProperties(handler, n)
    }

    def checkSideAny(index: Int) = checkSide(index, Direction.values: _*)

    def optSideAny(index: Int, default: Direction) =
      if (!isDefined(index)) default
      else checkSideAny(index)

    def checkSideExcept(index: Int, invalid: Direction*) = checkSide(index, Direction.values.filterNot(invalid.contains): _*)

    def optSideExcept(index: Int, default: Direction, invalid: Direction*) =
      if (!isDefined(index)) default
      else checkSideExcept(index, invalid: _*)

    def checkSideForAction(index: Int) = checkSide(index, Direction.SOUTH, Direction.UP, Direction.DOWN)

    def optSideForAction(index: Int, default: Direction) =
      if (!isDefined(index)) default
      else checkSideForAction(index)

    def checkSideForMovement(index: Int) = checkSide(index, Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.DOWN)

    def optSideForMovement(index: Int, default: Direction) =
      if (!isDefined(index)) default
      else checkSideForMovement(index)

    def checkSideForFace(index: Int, facing: Direction) = checkSideExcept(index, facing.getOpposite)

    def optSideForFace(index: Int, default: Direction) =
      if (!isDefined(index)) default
      else checkSideForAction(index)

    private def checkSide(index: Int, allowed: Direction*) = {
      val side = args.checkInteger(index)
      if (side < 0 || side > 5) {
        throw new IllegalArgumentException("invalid side")
      }
      val direction = Direction.from3DDataValue(side)
      if (allowed.isEmpty || (allowed contains direction)) direction
      else throw new IllegalArgumentException("unsupported side")
    }

    private def isDefined(index: Int) = index >= 0 && index < args.count()

    private def hasValue(index: Int) = args.checkAny(index) != null
  }

  @Deprecated
  class TankProperties(val capacity: Int, val contents: FluidStack)

}
