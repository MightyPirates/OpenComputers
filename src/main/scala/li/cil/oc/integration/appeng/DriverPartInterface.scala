package li.cil.oc.integration.appeng

import appeng.api.implementations.tiles.ISegmentedInventory
import appeng.api.networking.IGridHost
import appeng.api.networking.security.IActionHost
import appeng.api.parts.{IPartHost, PartItemStack}
import appeng.api.util.AEPartLocation
import li.cil.oc.api.driver
import li.cil.oc.api.driver.{EnvironmentProvider, NamedBlock}
import li.cil.oc.api.machine.{Arguments, Callback}
import li.cil.oc.api.machine.Context
import li.cil.oc.integration.ManagedTileEntityEnvironment
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object DriverPartInterface extends driver.DriverBlock {
  override def worksWith(world: World, pos: BlockPos, side: Direction): Boolean =
    world.getBlockEntity(pos) match {
      case container: IPartHost => {
        Direction.values.map(container.getPart).filter(p => p != null).map(_.getItemStack(PartItemStack.PICK)).exists(AEUtil.isPartInterface)
      }
      case _ => false
    }

  override def createEnvironment(world: World, pos: BlockPos, side: Direction): DriverPartInterface.Environment = {
    val host: IPartHost = world.getBlockEntity(pos).asInstanceOf[IPartHost]
    val tile = host.asInstanceOf[TileEntity with IPartHost with ISegmentedInventory with IActionHost with IGridHost]
    val aePos: AEPartLocation = side match {
      case Direction.EAST => AEPartLocation.WEST
      case Direction.WEST => AEPartLocation.EAST
      case Direction.NORTH => AEPartLocation.SOUTH
      case Direction.SOUTH => AEPartLocation.NORTH
      case Direction.UP => AEPartLocation.DOWN
      case Direction.DOWN => AEPartLocation.UP
    }
    new Environment(host, tile, aePos)
  }

  final class Environment(val host: IPartHost, val tile: TileEntity with IPartHost with ISegmentedInventory with IActionHost with IGridHost, val pos: AEPartLocation)
      extends ManagedTileEntityEnvironment[IPartHost](host, "me_interface")
      with NamedBlock with PartEnvironmentBase
      with NetworkControl[TileEntity with ISegmentedInventory with IActionHost with IGridHost]
  {
    override def preferredName = "me_interface"

    override def priority = 0

    @Callback(doc = "function(side:number[, slot:number]):table -- Get the configuration of the interface pointing in the specified direction.")
    def getInterfaceConfiguration(context: Context, args: Arguments): Array[AnyRef] = getPartConfig[ISegmentedInventory](context, args)

    @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number[, size:number]]):boolean -- Configure the interface pointing in the specified direction.")
    def setInterfaceConfiguration(context: Context, args: Arguments): Array[AnyRef] = setPartConfig[ISegmentedInventory](context, args)
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] =
      if (AEUtil.isPartInterface(stack))
        classOf[Environment]
      else null
  }

}