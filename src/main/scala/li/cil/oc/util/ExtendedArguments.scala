package li.cil.oc.util

import li.cil.oc.api.internal.MultiTank
import li.cil.oc.api.machine.Arguments
import net.minecraft.inventory.IInventory
import net.minecraft.util.EnumFacing
import net.minecraftforge.fluids.Fluid
import net.minecraftforge.fluids.capability.IFluidHandler
import net.minecraftforge.fluids.capability.IFluidTankProperties
import net.minecraftforge.items.IItemHandler

import scala.language.implicitConversions

object ExtendedArguments {

  implicit def extendedArguments(args: Arguments): ExtendedArguments = new ExtendedArguments(args)

  class ExtendedArguments(val args: Arguments) {
    def optItemCount(index: Int, default: Int = 64) =
      if (!isDefined(index) || !hasValue(index)) default
      else math.max(0, math.min(64, args.checkInteger(index)))

    def optFluidCount(index: Int, default: Int = Fluid.BUCKET_VOLUME) =
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
      if (tank < 0 || tank >= handler.getTankProperties.length) {
        throw new IllegalArgumentException("invalid tank index")
      }
      handler.getTankProperties()(tank)
    }

    def optTankProperties(handler: IFluidHandler, n: Int, default: IFluidTankProperties) = {
      if (!isDefined(n)) default
      else checkTankProperties(handler, n)
    }

    def checkSideAny(index: Int) = checkSide(index, EnumFacing.values: _*)

    def optSideAny(index: Int, default: EnumFacing) =
      if (!isDefined(index)) default
      else checkSideAny(index)

    def checkSideExcept(index: Int, invalid: EnumFacing*) = checkSide(index, EnumFacing.values.filterNot(invalid.contains): _*)

    def optSideExcept(index: Int, default: EnumFacing, invalid: EnumFacing*) =
      if (!isDefined(index)) default
      else checkSideExcept(index, invalid: _*)

    def checkSideForAction(index: Int) = checkSide(index, EnumFacing.SOUTH, EnumFacing.UP, EnumFacing.DOWN)

    def optSideForAction(index: Int, default: EnumFacing) =
      if (!isDefined(index)) default
      else checkSideForAction(index)

    def checkSideForMovement(index: Int) = checkSide(index, EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.UP, EnumFacing.DOWN)

    def optSideForMovement(index: Int, default: EnumFacing) =
      if (!isDefined(index)) default
      else checkSideForMovement(index)

    def checkSideForFace(index: Int, facing: EnumFacing) = checkSideExcept(index, facing.getOpposite)

    def optSideForFace(index: Int, default: EnumFacing) =
      if (!isDefined(index)) default
      else checkSideForAction(index)

    private def checkSide(index: Int, allowed: EnumFacing*) = {
      val side = args.checkInteger(index)
      if (side < 0 || side > 5) {
        throw new IllegalArgumentException("invalid side")
      }
      val direction = EnumFacing.getFront(side)
      if (allowed.isEmpty || (allowed contains direction)) direction
      else throw new IllegalArgumentException("unsupported side")
    }

    private def isDefined(index: Int) = index >= 0 && index < args.count()

    private def hasValue(index: Int) = args.checkAny(index) != null
  }

}
