package li.cil.oc.integration.appeng

import appeng.parts.automation.PartExportBus
import li.cil.oc.api.driver
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ResultWrapper._
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

object DriverExportBus extends driver.Block {
  type ExportBusTile = appeng.api.parts.IPartHost

  override def worksWith(world: World, x: Int, y: Int, z: Int) =
    world.getTileEntity(x, y, z) match {
      case container: ExportBusTile => ForgeDirection.VALID_DIRECTIONS.map(container.getPart).exists(_.isInstanceOf[PartExportBus])
      case _ => false
    }

  override def createEnvironment(world: World, x: Int, y: Int, z: Int) = new Environment(world.getTileEntity(x, y, z).asInstanceOf[ExportBusTile])

  class Environment(tileEntity: ExportBusTile) extends ManagedTileEntityEnvironment[ExportBusTile](tileEntity, "me_exportbus") with NamedBlock {
    override def preferredName = "me_exportbus"

    override def priority = 0

    @Callback(doc = "function(side:number, stack:table):boolean -- Configure the export bus point in the specified direction to export an item stack matching the specified descriptor.")
    def configure(context: Context, args: Arguments): Array[AnyRef] = {
      val side = args.checkSide(0, ForgeDirection.VALID_DIRECTIONS: _*)
      tileEntity.getPart(side) match {
        case export: PartExportBus =>
          val stack = args.checkItemStack(1)
          export.getInventoryByName("config").setInventorySlotContents(0, stack)
          result(true)
        case _ => result(Unit, "no export bus")
      }
    }
  }

}
