package li.cil.oc.integration.appeng

import appeng.api.AEApi
import appeng.parts.misc.PartInterface
import li.cil.oc.api.driver
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.internal.Database
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Component
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ResultWrapper._
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection


object DriverPartInterface extends driver.Block with EnvironmentAware{
  type InterfaceTile = appeng.api.parts.IPartHost
  
  override def worksWith(world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
    case container: InterfaceTile => ForgeDirection.VALID_DIRECTIONS.map(container.getPart).exists(_.isInstanceOf[PartInterface])
    case _ => false
  }
  
  override def createEnvironment(world: World, x: Int, y: Int, z: Int) = new Environment(world.getTileEntity(x, y, z).asInstanceOf[InterfaceTile])
  
  override def providedEnvironment(stack: ItemStack) =
    if (stack != null &&
        AEApi.instance != null &&
      AEApi.instance.parts() != null &&
      AEApi.instance.parts().partInterface != null &&
      stack.getItem == AEApi.instance().parts().partInterface.item() &&
      AEApi.instance().parts().partInterface.stack(1) != null &&
      AEApi.instance().parts().partInterface.stack(1).getItemDamage == stack.getItemDamage) classOf[Environment]
    else null
  
  
  class Environment(host: InterfaceTile) extends ManagedTileEntityEnvironment[InterfaceTile](host, "me_interface") with NamedBlock {
    override def preferredName = "me_interface"

    override def priority = 0
    
    @Callback(doc = "function(side:number[, slot:number]):table -- Get the configuration of the interface pointing in the specified direction.")
    def getInterfaceConfiguration(context: Context, args: Arguments): Array[AnyRef] = {
      val side = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
      host.getPart(side) match {
        case interface: PartInterface =>
          val config = interface.getInventoryByName("config")
          val slot = args.optSlot(config, 2, 0)
          val stack = config.getStackInSlot(slot)
          result(stack)
        case _ => result(Unit, "no interface")
      }
    }
    
    @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number, size:number]):boolean -- Configure the interface pointing in the specified direction..")
    def setInterfaceConfiguration(context: Context, args: Arguments): Array[AnyRef] = {
      val side = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
      host.getPart(side) match {
        case interface: PartInterface =>
          val config = interface.getInventoryByName("config")
          val slot = if (args.count > 4 || args.count < 4) args.optSlot(config, 1, 0) else 0
          val stack = if (args.count > 2) {
            val (address, entry, size) =
              if (args.count > 4) (args.checkString(2), args.checkInteger(3), args.checkInteger(4))
              else (args.checkString(1), args.checkInteger(2), args.checkInteger(3))
            node.network.node(address) match {
              case component: Component => component.host match {
                case database: Database => {
                  val s = database.getStackInSlot(entry - 1)
                  if (s == null)
                    null
                  else{
                    if (size <= 0)
                      null
                    s.stackSize = Math.min(size, s.getMaxStackSize)
                    s
                  }
                }
                case _ => throw new IllegalArgumentException("not a database")
              }
              case _ => throw new IllegalArgumentException("no such component")
            }
          }
          else null
          config.setInventorySlotContents(slot, stack)
          context.pause(0.5)
          result(true)
        case _ => result(Unit, "no interface")
      }
    }
    
  }
    

}