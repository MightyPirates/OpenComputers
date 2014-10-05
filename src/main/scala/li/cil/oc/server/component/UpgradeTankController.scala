package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import li.cil.oc.common.tileentity.Robot
import li.cil.oc.util.ExtendedArguments._
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.fluids.FluidContainerRegistry
import net.minecraftforge.fluids.IFluidContainerItem
import net.minecraftforge.fluids.IFluidHandler

class UpgradeTankController(val owner: EnvironmentHost with Robot) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("tank_controller", Visibility.Neighbors).
    create()

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function(side:number):number -- Get the capacity of the tank on the specified side of the robot. Back refers to the robot's own selected tank.""")
  def getTankCapacity(context: Context, args: Arguments): Array[AnyRef] = {
    val facing = checkSideForTank(args, 0)
    if (facing == owner.facing.getOpposite) result(owner.getFluidTank(owner.selectedTank).fold(0)(_.getCapacity))
    else owner.world.getTileEntity(math.floor(owner.xPosition).toInt + facing.offsetX, math.floor(owner.yPosition).toInt + facing.offsetY, math.floor(owner.zPosition).toInt + facing.offsetZ) match {
      case handler: IFluidHandler =>
        result((owner.getFluidTank(owner.selectedTank) match {
          case Some(tank) => handler.getTankInfo(facing.getOpposite).filter(info => info.fluid == null || info.fluid.isFluidEqual(tank.getFluid))
          case _ => handler.getTankInfo(facing.getOpposite)
        }).map(_.capacity).foldLeft(0)((max, capacity) => math.max(max, capacity)))
      case _ => result(Unit, "no tank")
    }
  }

  @Callback(doc = """function(side:number):table -- Get a description of the fluid in the the tank on the specified side of the robot. Back refers to the robot's own selected tank.""")
  def getFluidInTank(context: Context, args: Arguments): Array[AnyRef] = if (Settings.get.allowItemStackInspection) {
    val facing = checkSideForTank(args, 0)
    if (facing == owner.facing.getOpposite) result(owner.getFluidTank(owner.selectedTank).map(_.getFluid).orNull)
    else owner.world.getTileEntity(math.floor(owner.xPosition).toInt + facing.offsetX, math.floor(owner.yPosition).toInt + facing.offsetY, math.floor(owner.zPosition).toInt + facing.offsetZ) match {
      case handler: IFluidHandler => result(Option(handler.getTankInfo(facing.getOpposite)).map(_.map(_.fluid)).orNull)
      case _ => result(Unit, "no tank")
    }
  }
  else result(Unit, "not enabled in config")

  @Callback(doc = """function([amount:number]):boolean -- Transfers fluid from a tank in the selected inventory slot to the selected tank.""")
  def drain(context: Context, args: Arguments): Array[AnyRef] = {
    val amount = args.optionalFluidCount(0)
    owner.getFluidTank(owner.selectedTank) match {
      case Some(tank) =>
        owner.getStackInSlot(owner.selectedSlot) match {
          case stack: ItemStack =>
            if (FluidContainerRegistry.isFilledContainer(stack)) {
              val contents = FluidContainerRegistry.getFluidForFilledItem(stack)
              val container = stack.getItem.getContainerItem(stack)
              if (tank.getCapacity - tank.getFluidAmount < contents.amount) {
                result(Unit, "tank is full")
              }
              else if (tank.fill(contents, false) < contents.amount) {
                result(Unit, "incompatible fluid")
              }
              else {
                tank.fill(contents, true)
                owner.decrStackSize(owner.selectedSlot, 1)
                owner.player().inventory.addItemStackToInventory(container)
                result(true)
              }
            }
            else stack.getItem match {
              case container: IFluidContainerItem =>
                val drained = container.drain(stack, amount, false)
                val transferred = tank.fill(drained, true)
                if (transferred > 0) {
                  container.drain(stack, transferred, true)
                  result(true)
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
    val amount = args.optionalFluidCount(0)
    owner.getFluidTank(owner.selectedTank) match {
      case Some(tank) =>
        owner.getStackInSlot(owner.selectedSlot) match {
          case stack: ItemStack =>
            if (FluidContainerRegistry.isEmptyContainer(stack)) {
              val drained = tank.drain(amount, false)
              val filled = FluidContainerRegistry.fillFluidContainer(drained, stack)
              if (filled == null) {
                result(Unit, "tank is empty")
              }
              else {
                tank.drain(FluidContainerRegistry.getFluidForFilledItem(filled).amount, true)
                owner.decrStackSize(owner.selectedSlot, 1)
                owner.player().inventory.addItemStackToInventory(filled)
                result(true)
              }
            }
            else stack.getItem match {
              case container: IFluidContainerItem =>
                val drained = tank.drain(amount, false)
                val transferred = container.fill(stack, drained, true)
                if (transferred > 0) {
                  tank.drain(transferred, true)
                  result(true)
                }
                else result(Unit, "incompatible or no fluid")
              case _ => result(Unit, "item is full or not a fluid container")
            }
          case _ => result(Unit, "nothing selected")
        }
      case _ => result(Unit, "no tank")
    }
  }

  private def checkSideForTank(args: Arguments, n: Int) = owner.toGlobal(args.checkSide(n, ForgeDirection.SOUTH, ForgeDirection.NORTH, ForgeDirection.UP, ForgeDirection.DOWN))
}
