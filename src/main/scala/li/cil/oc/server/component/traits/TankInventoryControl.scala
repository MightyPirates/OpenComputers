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

trait TankInventoryControl extends WorldAware with InventoryAware with TankAware {
  @Callback(doc = """function([slot:number]):number -- Get the amount of fluid in the tank item in the specified slot or the selected slot.""")
  def getTankLevelInSlot(context: Context, args: Arguments): Array[AnyRef] =
    withFluidInfo(optSlot(args, 0), (fluid, _) => result(fluid.fold(0)(_.amount)))

  @Callback(doc = """function([slot:number]):number -- Get the capacity of the tank item in the specified slot of the robot or the selected slot.""")
  def getTankCapacityInSlot(context: Context, args: Arguments): Array[AnyRef] =
    withFluidInfo(optSlot(args, 0), (_, capacity) => result(capacity))

  @Callback(doc = """function([slot:number]):table -- Get a description of the fluid in the tank item in the specified slot or the selected slot.""")
  def getFluidInTankInSlot(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    withFluidInfo(optSlot(args, 0), (fluid, _) => result(fluid.orNull))
  }
  else result(Unit, "not enabled in config")

  @Callback(doc = """function([tank:number]):table -- Get a description of the fluid in the tank in the specified slot or the selected slot.""")
  def getFluidInInternalTank(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    result(Option(tank.getFluidTank(optTank(args, 0))).map(_.getFluid).orNull)
  }
  else result(Unit, "not enabled in config")

  @Callback(doc = """function([amount:number]):boolean -- Transfers fluid from a tank in the selected inventory slot to the selected tank.""")
  def drain(context: Context, args: Arguments): Array[AnyRef] = {
    val amount = args.optFluidCount(0)
    Option(tank.getFluidTank(selectedTank)) match {
      case Some(into) => inventory.getStackInSlot(selectedSlot) match {
        case stack: ItemStack =>
          Option(FluidUtils.fluidHandlerOf(stack)) match {
            case Some(handler) =>
              val drained = handler.drain(amount, false)
              val transferred = into.fill(drained, true)
              if (transferred > 0) {
                handler.drain(transferred, true)
                inventory.setInventorySlotContents(selectedSlot, handler.getContainer)
                result(true, transferred)
              }
              else result(Unit, "incompatible or no fluid")
            case _ => result(Unit, "item is not a fluid container")
          }
        case _ => result(Unit, "nothing selected")
      }
      case _ => result(Unit, "no tank")
    }
  }

  @Callback(doc = """function([amount:number]):boolean -- Transfers fluid from the selected tank to a tank in the selected inventory slot.""")
  def fill(context: Context, args: Arguments): Array[AnyRef] = {
    val amount = args.optFluidCount(0)
    Option(tank.getFluidTank(selectedTank)) match {
      case Some(from) => inventory.getStackInSlot(selectedSlot) match {
        case stack: ItemStack =>
          Option(FluidUtils.fluidHandlerOf(stack)) match {
            case Some(handler) =>
              val drained = from.drain(amount, false)
              val transferred = handler.fill(drained, true)
              if (transferred > 0) {
                from.drain(transferred, true)
                inventory.setInventorySlotContents(selectedSlot, handler.getContainer)
                result(true, transferred)
              }
              else result(Unit, "incompatible or no fluid")
            case _ => result(Unit, "item is not a fluid container")
          }
        case _ => result(Unit, "nothing selected")
      }
      case _ => result(Unit, "no tank")
    }
  }

  private def withFluidInfo(slot: Int, f: (Option[FluidStack], Int) => Array[AnyRef]) = {
    def fluidInfo(stack: ItemStack) = Option(FluidUtils.fluidHandlerOf(stack)) match {
      case Some(handler) if handler.getTankProperties.length > 0 =>
        val props = handler.getTankProperties()(0)
        Option((Option(props.getContents), props.getCapacity))
      case _ => None
    }

    inventory.getStackInSlot(slot) match {
      case stack: ItemStack => fluidInfo(stack) match {
        case Some((fluid, capacity)) => f(fluid, capacity)
        case _ => result(Unit, "item is not a fluid container")
      }
    }
  }
}
