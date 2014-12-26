package li.cil.oc.util

import li.cil.oc.api.internal.Robot
import li.cil.oc.api.machine.Arguments
import li.cil.oc.common.inventory.MultiTank
import net.minecraft.inventory.IInventory
import net.minecraft.util.EnumFacing

import scala.language.implicitConversions

object ExtendedArguments {

  implicit def extendedArguments(args: Arguments): ExtendedArguments = new ExtendedArguments(args)

  class ExtendedArguments(val args: Arguments) {
    def optionalItemCount(n: Int) =
      if (args.count > n && args.checkAny(n) != null) {
        math.max(0, math.min(64, args.checkInteger(n)))
      }
      else 64

    def optionalFluidCount(n: Int) =
      if (args.count > n && args.checkAny(n) != null) {
        math.max(0, args.checkInteger(n))
      }
      else 1000

    def checkSlot(inventory: IInventory, n: Int) = {
      val slot = args.checkInteger(n) - 1
      if (slot < 0 || slot >= inventory.getSizeInventory) {
        throw new IllegalArgumentException("invalid slot")
      }
      slot
    }

    def optSlot(inventory: IInventory, n: Int, default: Int) = {
      if (n >= 0 && n < args.count()) checkSlot(inventory, n)
      else default
    }

    def checkSlot(robot: Robot, n: Int) = {
      val slot = args.checkInteger(n) - 1
      if (slot < 0 || slot >= robot.inventorySize) {
        throw new IllegalArgumentException("invalid slot")
      }
      slot + 1 + robot.containerCount
    }

    def checkTank(multi: MultiTank, n: Int) = {
      val tank = args.checkInteger(n) - 1
      if (tank < 0 || tank >= multi.tankCount) {
        throw new IllegalArgumentException("invalid tank index")
      }
      tank
    }

    def checkSideForAction(n: Int) = checkSide(n, EnumFacing.SOUTH, EnumFacing.UP, EnumFacing.DOWN)

    def checkSideForMovement(n: Int) = checkSide(n, EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.UP, EnumFacing.DOWN)

    def checkSideForFace(n: Int, facing: EnumFacing) = checkSide(n, EnumFacing.values.filter(_ != facing.getOpposite): _*)

    def checkSide(n: Int, allowed: EnumFacing*) = {
      val side = args.checkInteger(n)
      if (side < 0 || side > 5) {
        throw new IllegalArgumentException("invalid side")
      }
      val direction = EnumFacing.getFront(side)
      if (allowed.isEmpty || (allowed contains direction)) direction
      else throw new IllegalArgumentException("unsupported side")
    }
  }

}
