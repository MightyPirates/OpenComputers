package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.server.component.result
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.FluidUtils
import li.cil.oc.util.InventoryUtils
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction
import net.minecraftforge.fluids.capability.CapabilityFluidHandler

trait TankInventoryControl extends WorldAware with InventoryAware with TankAware {
  @Callback(doc = """function([slot:number]):number -- Get the amount of fluid in the tank item in the specified slot or the selected slot.""")
  def getTankLevelInSlot(context: Context, args: Arguments): Array[AnyRef] =
    withFluidInfo(optSlot(args, 0), (fluid, _) => result(fluid.fold(0)(_.getAmount)))

  @Callback(doc = """function([slot:number]):number -- Get the capacity of the tank item in the specified slot of the robot or the selected slot.""")
  def getTankCapacityInSlot(context: Context, args: Arguments): Array[AnyRef] =
    withFluidInfo(optSlot(args, 0), (_, capacity) => result(capacity))

  @Callback(doc = """function([slot:number]):table -- Get a description of the fluid in the tank item in the specified slot or the selected slot.""")
  def getFluidInTankInSlot(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    withFluidInfo(optSlot(args, 0), (fluid, _) => result(fluid.orNull))
  }
  else result((), "not enabled in config")

  @Callback(doc = """function([tank:number]):table -- Get a description of the fluid in the tank in the specified slot or the selected slot.""")
  def getFluidInInternalTank(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    result(Option(tank.getFluidTank(optTank(args, 0))).map(_.getFluid).orNull)
  }
  else result((), "not enabled in config")

  @Callback(doc = """function([amount:number]):boolean -- Transfers fluid from a tank in the selected inventory slot to the selected tank.""")
  def drain(context: Context, args: Arguments): Array[AnyRef] = {
    val amount = args.optFluidCount(0)
    Option(tank.getFluidTank(selectedTank)) match {
      case Some(into) => inventory.getItem(selectedSlot) match {
        case stack: ItemStack =>
          Option(FluidUtils.fluidHandlerOf(stack)) match {
            case Some(handler) =>
              val drained = handler.drain(amount, FluidAction.SIMULATE)
              val transferred = into.fill(drained, FluidAction.EXECUTE)
              if (transferred > 0) {
                handler.drain(transferred, FluidAction.EXECUTE)
                inventory.setItem(selectedSlot, handler.getContainer)
                result(true, transferred)
              }
              else result((), "incompatible or no fluid")
            case _ => result((), "item is not a fluid container")
          }
        case _ => result((), "nothing selected")
      }
      case _ => result((), "no tank")
    }
  }

  @Callback(doc = """function([amount:number]):boolean -- Transfers fluid from the selected tank to a tank in the selected inventory slot.""")
  def fill(context: Context, args: Arguments): Array[AnyRef] = {
    val amount = args.optFluidCount(0)
    Option(tank.getFluidTank(selectedTank)) match {
      case Some(from) => inventory.getItem(selectedSlot) match {
        case stack: ItemStack =>
          Option(FluidUtils.fluidHandlerOf(stack)) match {
            case Some(handler) =>
              val drained = from.drain(amount, FluidAction.SIMULATE)
              val transferred = handler.fill(drained, FluidAction.EXECUTE)
              if (transferred > 0) {
                from.drain(transferred, FluidAction.EXECUTE)
                inventory.setItem(selectedSlot, handler.getContainer)
                result(true, transferred)
              }
              else result((), "incompatible or no fluid")
            case _ => result((), "item is not a fluid container")
          }
        case _ => result((), "nothing selected")
      }
      case _ => result((), "no tank")
    }
  }

  private def withFluidInfo(slot: Int, f: (Option[FluidStack], Int) => Array[AnyRef]) = {
    def fluidInfo(stack: ItemStack) = Option(FluidUtils.fluidHandlerOf(stack)) match {
      case Some(handler) if handler.getTanks > 0 =>
        Option((Option(handler.getFluidInTank(0)), handler.getTankCapacity(0)))
      case _ => None
    }

    inventory.getItem(slot) match {
      case stack: ItemStack => fluidInfo(stack) match {
        case Some((fluid, capacity)) => f(fluid, capacity)
        case _ => result((), "item is not a fluid container")
      }
    }
  }
}
