package li.cil.oc.server.component.traits

import li.cil.oc.Settings
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.server.component.result
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import net.minecraft.item.ItemStack
import net.minecraftforge.fluids.FluidContainerRegistry
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidContainerItem

trait TankInventoryControl extends WorldAware with InventoryAware with TankAware {
  @Callback(doc = """function([slot:number]):number -- Get the amount of fluid in the tank item in the specified slot or the selected slot.""")
  def getTankLevelInSlot(context: Context, args: Arguments): Array[AnyRef] =
    withFluidInfo(optSlot(args, 0), (fluid, _) => result(fluid.amount))

  @Callback(doc = """function([slot:number]):number -- Get the capacity of the tank item in the specified slot of the robot or the selected slot.""")
  def getTankCapacityInSlot(context: Context, args: Arguments): Array[AnyRef] =
    withFluidInfo(optSlot(args, 0), (_, capacity) => result(capacity))

  @Callback(doc = """function([slot:number]):table -- Get a description of the fluid in the tank item in the specified slot or the selected slot.""")
  def getFluidInTankInSlot(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    withFluidInfo(optSlot(args, 0), (fluid, _) => result(fluid))
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
          if (FluidContainerRegistry.isFilledContainer(stack)) {
            val contents = FluidContainerRegistry.getFluidForFilledItem(stack)
            val container = stack.getItem.getContainerItem(stack)
            if (into.getCapacity - into.getFluidAmount < contents.amount) {
              result(Unit, "tank is full")
            }
            else if (into.fill(contents, false) < contents.amount) {
              result(Unit, "incompatible fluid")
            }
            else {
              into.fill(contents, true)
              inventory.decrStackSize(selectedSlot, 1)
              InventoryUtils.insertIntoInventory(container, inventory, slots = Option(insertionSlots))
              if (container.stackSize > 0) {
                InventoryUtils.spawnStackInWorld(position, container)
              }
              result(true, contents.amount)
            }
          }
          else stack.getItem match {
            case from: IFluidContainerItem =>
              val drained = from.drain(stack, amount, false)
              val transferred = into.fill(drained, true)
              if (transferred > 0) {
                from.drain(stack, transferred, true)
                result(true, transferred)
              }
              else result(Unit, "incompatible or no fluid")
            case _ => result(Unit, "item is empty or not a fluid container")
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
          if (FluidContainerRegistry.isEmptyContainer(stack)) {
            val drained = from.drain(amount, false)
            val filled = FluidContainerRegistry.fillFluidContainer(drained, stack)
            if (filled == null) {
              result(Unit, "tank is empty")
            }
            else {
              val amount = FluidContainerRegistry.getFluidForFilledItem(filled).amount
              from.drain(amount, true)
              inventory.decrStackSize(selectedSlot, 1)
              InventoryUtils.insertIntoInventory(filled, inventory, slots = Option(insertionSlots))
              if (filled.stackSize > 0) {
                InventoryUtils.spawnStackInWorld(position, filled)
              }
              result(true, amount)
            }
          }
          else stack.getItem match {
            case into: IFluidContainerItem =>
              val drained = from.drain(amount, false)
              val transferred = into.fill(stack, drained, true)
              if (transferred > 0) {
                from.drain(transferred, true)
                result(true, transferred)
              }
              else result(Unit, "incompatible or no fluid")
            case _ => result(Unit, "item is full or not a fluid container")
          }
        case _ => result(Unit, "nothing selected")
      }
      case _ => result(Unit, "no tank")
    }
  }

  private def withFluidInfo(slot: Int, f: (FluidStack, Int) => Array[AnyRef]) = {
    def fluidInfo(stack: ItemStack) = {
      if (FluidContainerRegistry.isFilledContainer(stack)) {
        Option((FluidContainerRegistry.getFluidForFilledItem(stack), FluidContainerRegistry.getContainerCapacity(stack)))
      }
      else if (FluidContainerRegistry.isEmptyContainer(stack)) {
        Option((new FluidStack(0, 0), FluidContainerRegistry.getContainerCapacity(stack)))
      }
      else stack.getItem match {
        case from: IFluidContainerItem => Option((from.getFluid(stack), from.getCapacity(stack)))
        case _ => None
      }
    }
    inventory.getStackInSlot(slot) match {
      case stack: ItemStack => fluidInfo(stack) match {
        case Some((fluid, capacity)) => f(fluid, capacity)
        case _ => result(Unit, "item is not a fluid container")
      }
    }
  }
}
