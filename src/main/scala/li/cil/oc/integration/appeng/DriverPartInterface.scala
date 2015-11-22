package li.cil.oc.integration.appeng

import appeng.api.parts.IPartHost
import appeng.parts.misc.PartInterface
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

object DriverPartInterface extends driver.Block {
  override def worksWith(world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case container: IPartHost => ForgeDirection.VALID_DIRECTIONS.map(container.getPart).exists(_.isInstanceOf[PartInterface])
      case _ => false
    }

  override def createEnvironment(world: World, x: Int, y: Int, z: Int) = new Environment(world.getTileEntity(x, y, z).asInstanceOf[IPartHost])

  final class Environment(val host: IPartHost) extends ManagedTileEntityEnvironment[IPartHost](host, "me_interface") with NamedBlock with PartEnvironmentBase {
    override def preferredName = "me_interface"

    override def priority = 0

    @Callback(doc = "function(side:number[, slot:number]):table -- Get the configuration of the interface pointing in the specified direction.")
    def getInterfaceConfiguration(context: Context, args: Arguments): Array[AnyRef] = getPartConfig[PartInterface](context, args)

    @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number[, size:number]]):boolean -- Configure the interface pointing in the specified direction.")
    def setInterfaceConfiguration(context: Context, args: Arguments): Array[AnyRef] = setPartConfig[PartInterface](context, args)
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (AEUtil.isPartInterface(stack))
        classOf[Environment]
      else null
  }

}