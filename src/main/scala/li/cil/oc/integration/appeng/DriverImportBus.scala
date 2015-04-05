package li.cil.oc.integration.appeng

import appeng.api.AEApi
import appeng.api.config.Actionable
import appeng.api.config.FuzzyMode
import appeng.api.config.Settings
import appeng.api.config.Upgrades
import appeng.api.networking.security.MachineSource
import appeng.parts.automation.PartImportBus
import appeng.util.Platform
import li.cil.oc.api.driver
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.internal.Database
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.Component
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ResultWrapper._
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

object DriverImportBus extends driver.Block with EnvironmentAware {
  type ImportBusTile = appeng.api.parts.IPartHost

  override def worksWith(world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case container: ImportBusTile => ForgeDirection.VALID_DIRECTIONS.map(container.getPart).exists(_.isInstanceOf[PartImportBus])
      case _ => false
    }

  override def createEnvironment(world: World, x: Int, y: Int, z: Int) = new Environment(world.getTileEntity(x, y, z).asInstanceOf[ImportBusTile])

  override def providedEnvironment(stack: ItemStack) = 
    if (stack != null &&
        AEApi.instance != null &&
      AEApi.instance.parts() != null &&
      AEApi.instance.parts().partImportBus != null &&
      stack.getItem == AEApi.instance().parts().partImportBus.item() &&
      AEApi.instance().parts().partImportBus.stack(1) != null &&
      AEApi.instance().parts().partImportBus.stack(1).getItemDamage == stack.getItemDamage) classOf[Environment]
    else null
  
  class Environment(host: ImportBusTile) extends ManagedTileEntityEnvironment[ImportBusTile](host, "me_importbus") with NamedBlock {
    override def preferredName = "me_importbus"

    override def priority = 1

    @Callback(doc = "function(side:number, [ slot:number]):boolean -- Get the configuration of the import bus pointing in the specified direction.")
    def getImportConfiguration(context: Context, args: Arguments): Array[AnyRef] = {
      val side = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
      host.getPart(side) match {
        case export: PartImportBus =>
          val config = export.getInventoryByName("config")
          val slot = args.optSlot(config, 2, 0)
          val stack = config.getStackInSlot(slot)
          result(stack)
        case _ => result(Unit, "no export bus")
      }
    }

    @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number]):boolean -- Configure the import bus pointing in the specified direction to export item stacks matching the specified descriptor.")
    def setImportConfiguration(context: Context, args: Arguments): Array[AnyRef] = {
      val side = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
      host.getPart(side) match {
        case export: PartImportBus =>
          val config = export.getInventoryByName("config")
          val slot = if (args.count > 3 || args.count < 3) args.optSlot(config, 1, 0) else 0
          val stack = if (args.count > 2) {
            val (address, entry) =
              if (args.count > 3) (args.checkString(2), args.checkInteger(3))
              else (args.checkString(1), args.checkInteger(2))
            node.network.node(address) match {
              case component: Component => component.host match {
                case database: Database => database.getStackInSlot(entry - 1)
                case _ => throw new IllegalArgumentException("not a database")
              }
              case _ => throw new IllegalArgumentException("no such component")
            }
          }
          else null
          config.setInventorySlotContents(slot, stack)
          context.pause(0.5)
          result(true)
        case _ => result(Unit, "no export bus")
      }
    }
  }
}
