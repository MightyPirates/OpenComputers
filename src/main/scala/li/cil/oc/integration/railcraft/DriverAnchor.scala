package li.cil.oc.integration.railcraft


import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import mods.railcraft.common.blocks.machine.alpha.{EnumMachineAlpha, TileAnchorWorld}
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

object DriverAnchor extends DriverSidedTileEntity {
  def getTileEntityClass: Class[_] = classOf[TileAnchorWorld]

  def createEnvironment(world: World, x: Int, y: Int, z: Int, side: ForgeDirection): ManagedEnvironment =
    new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileAnchorWorld])

  final class Environment(val tile: TileAnchorWorld) extends ManagedTileEntityEnvironment[TileAnchorWorld](tile, "anchor") with NamedBlock {
    override def preferredName = "anchor"
    override def priority = 5

    @Callback(doc = "function():int -- Get remaining anchor time in ticks.")
    def getFuel(context: Context, args: Arguments): Array[AnyRef] = result(tile.getAnchorFuel)

    @Callback(doc = "function():string -- Get the  anchor owner name.")
    def getOwner(context: Context, args: Arguments): Array[AnyRef] = result(tile.getOwner.getName)

    @Callback(doc = "function():string -- Get the anchor type.")
    def getType(context: Context, args: Arguments): Array[AnyRef] = tile.getMachineType match {
      case EnumMachineAlpha.WORLD_ANCHOR => result("world")
      case EnumMachineAlpha.ADMIN_ANCHOR => result("admin")
      case EnumMachineAlpha.PERSONAL_ANCHOR => result("personal")
      case _ => result("passive")
    }

    @Callback(doc = "function():table -- Get the anchor input slot contents.")
    def getFuelSlotContents(context: Context, args: Arguments): Array[AnyRef] = result(tile.getStackInSlot(0))

    @Callback(doc = "function():boolean -- If the anchor is disabled with redstone.")
    def isDisabled(context: Context, args: Arguments): Array[AnyRef] = result(tile.isPowered)
  }
}
