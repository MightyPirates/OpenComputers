package li.cil.oc.integration.appeng

import appeng.api.implementations.tiles.ISegmentedInventory
import appeng.api.parts.{IPartHost, PartItemStack}
import li.cil.oc.api.driver
import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.integration.ManagedTileEntityEnvironment
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object DriverImportBus extends driver.SidedBlock {
  override def worksWith(world: World, pos: BlockPos, side: EnumFacing) =
    world.getTileEntity(pos) match {
      case container: IPartHost => EnumFacing.VALUES.map(container.getPart).filter(obj => { obj != null }).map(_.getItemStack(PartItemStack.PICK)).filter(obj => { obj != null }).exists(AEUtil.isImportBus)
      case _ => false
    }

  override def createEnvironment(world: World, pos: BlockPos, side: EnumFacing) = new Environment(world.getTileEntity(pos).asInstanceOf[IPartHost])

  final class Environment(val host: IPartHost) extends ManagedTileEntityEnvironment[IPartHost](host, "me_importbus") with NamedBlock with PartEnvironmentBase {
    override def preferredName = "me_importbus"

    override def priority = 1

    @Callback(doc = "function(side:number[, slot:number]):boolean -- Get the configuration of the import bus pointing in the specified direction.")
    def getImportConfiguration(context: Context, args: Arguments): Array[AnyRef] = getPartConfig[ISegmentedInventory](context, args)

    @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number]):boolean -- Configure the import bus pointing in the specified direction to import item stacks matching the specified descriptor.")
    def setImportConfiguration(context: Context, args: Arguments): Array[AnyRef] = setPartConfig[ISegmentedInventory](context, args)
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (AEUtil.isImportBus(stack))
        classOf[Environment]
      else null
  }

}
