package li.cil.oc.integration.railcraft


import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import mods.railcraft.common.blocks.machine.worldspike.{TileWorldspike, WorldspikeVariant}
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

import java.util.Objects

object DriverWorldspike extends DriverSidedTileEntity {
  def getTileEntityClass: Class[_] = classOf[TileWorldspike]

  def createEnvironment(world: World, pos: BlockPos, side: EnumFacing): ManagedEnvironment =
    new Environment(world.getTileEntity(pos).asInstanceOf[TileWorldspike])

  final class Environment(val tile: TileWorldspike) extends ManagedTileEntityEnvironment[TileWorldspike](tile, "worldspike") with NamedBlock {
    override def preferredName = "worldspike"
    override def priority = 5

    @Callback(doc = "function():int -- Get the amount of fuel.")
    def getFuel(context: Context, args: Arguments): Array[AnyRef] = result(tile.getFuelAmount)

    @Callback(doc = "function():string -- Get the anchor owner name.")
    def getOwner(context: Context, args: Arguments): Array[AnyRef] =
      if (tile.getOwner == null || tile.getOwner.getName == null || Objects.equals(tile.getOwner.getName, "[unknown]"))
        result()
      else
        result(tile.getOwner.getName)

    @Callback(doc = "function():string -- Get the anchor type.")
    def getType(context: Context, args: Arguments): Array[AnyRef] = tile.getMachineType match {
      case WorldspikeVariant.STANDARD => result("world")
      case WorldspikeVariant.ADMIN => result("admin")
      case WorldspikeVariant.PERSONAL => result("personal")
      case WorldspikeVariant.PASSIVE => result("passive")
      case _ => result("missing")
    }

    @Callback(doc = "function():table -- Get the anchor fuel slot's contents.")
    def getFuelSlotContents(context: Context, args: Arguments): Array[AnyRef] = {
      if (tile.needsFuel())
        result(tile.getStackInSlot(0))
      else
        result()
    }

    @Callback(doc = "function():boolean -- If the anchor is disabled (powered by redstone).")
    def isDisabled(context: Context, args: Arguments): Array[AnyRef] = result(tile.isPowered)
  }
}
