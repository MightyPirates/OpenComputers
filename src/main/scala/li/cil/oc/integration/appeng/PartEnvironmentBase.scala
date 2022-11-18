package li.cil.oc.integration.appeng

import appeng.api.implementations.tiles.ISegmentedInventory
import appeng.api.parts.IPartHost
import li.cil.oc.api.internal.Database
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Component
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.item.ItemStack

import scala.reflect.ClassTag

trait PartEnvironmentBase extends ManagedEnvironment {
  def host: IPartHost

  // function(side:number[, slot:number]):table
  def getPartConfig[PartType <: ISegmentedInventory : ClassTag](context: Context, args: Arguments): Array[AnyRef] = {
    val side = args.checkSideAny(0)
    host.getPart(side) match {
      case part: PartType =>
        val config = part.getInventoryByName("config")
        val slot = args.optSlot(config, 1, 0)
        val stack = config.getStackInSlot(slot)
        result(stack)
      case _ => result((), "no matching part")
    }
  }

  // function(side:number[, slot:number][, database:address, entry:number[, size:number]]):boolean
  def setPartConfig[PartType <: ISegmentedInventory : ClassTag](context: Context, args: Arguments): Array[AnyRef] = {
    val side = args.checkSideAny(0)
    host.getPart(side) match {
      case part: PartType =>
        val config = part.getInventoryByName("config")
        val slot = if (args.isString(1)) 0 else args.optSlot(config, 1, 0)
        val stack = if (args.count > 2) {
          val (address, entry, size) =
            if (args.isString(1)) (args.checkString(1), args.checkInteger(2), args.optInteger(3, 1))
            else (args.checkString(2), args.checkInteger(3), args.optInteger(4, 1))

          node.network.node(address) match {
            case component: Component => component.host match {
              case database: Database =>
                val dbStack = database.getStackInSlot(entry - 1)
                if (dbStack == null || size < 1 || dbStack.isEmpty) ItemStack.EMPTY
                else {
                  dbStack.setCount(math.min(size, dbStack.getMaxStackSize))
                  dbStack
                }
              case _ => throw new IllegalArgumentException("not a database")
            }
            case _ => throw new IllegalArgumentException("no such component")
          }
        }
        else ItemStack.EMPTY
        config.extractItem(slot, config.getStackInSlot(slot).getCount, false)
        config.insertItem(slot, stack, false)
        context.pause(0.5)
        result(true)
      case _ => result((), "no matching part")
    }
  }
}
