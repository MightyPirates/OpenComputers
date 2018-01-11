package li.cil.oc.integration.appeng

import appeng.api.parts.IPartHost
import appeng.parts.automation.PartImportBus
import li.cil.oc.api.driver
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.integration.ManagedTileEntityEnvironment
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

object DriverImportBus extends driver.SidedBlock {
  override def worksWith(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getTileEntity(x, y, z) match {
      case container: IPartHost => ForgeDirection.VALID_DIRECTIONS.map(container.getPart).filter(obj => { obj != null }).exists(_.isInstanceOf[PartImportBus])
      case _ => false
    }

  override def createEnvironment(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) = new Environment(world.getTileEntity(x, y, z).asInstanceOf[IPartHost])

  final class Environment(val host: IPartHost) extends ManagedTileEntityEnvironment[IPartHost](host, "me_importbus") with NamedBlock with PartEnvironmentBase {
    override def preferredName = "me_importbus"

    override def priority = 1

    @Callback(doc = "function(side:number[, slot:number]):boolean -- Get the configuration of the import bus pointing in the specified direction.")
    def getImportConfiguration(context: Context, args: Arguments): Array[AnyRef] = getPartConfig[PartImportBus](context, args)

    @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number]):boolean -- Configure the import bus pointing in the specified direction to import item stacks matching the specified descriptor.")
    def setImportConfiguration(context: Context, args: Arguments): Array[AnyRef] = setPartConfig[PartImportBus](context, args)
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (AEUtil.isImportBus(stack))
        classOf[Environment]
      else null
  }

}
