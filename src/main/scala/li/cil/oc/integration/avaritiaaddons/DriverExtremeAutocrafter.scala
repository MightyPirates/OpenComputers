package li.cil.oc.integration.avaritiaaddons

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.internal.Database
import li.cil.oc.api.machine.{Arguments, Callback, Context}
import li.cil.oc.api.network.{Component, ManagedEnvironment}
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.item.ItemStack
import net.minecraft.item.Item
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import wanion.avaritiaddons.block.extremeautocrafter.{BlockExtremeAutoCrafter, TileEntityExtremeAutoCrafter}

object DriverExtremeAutocrafter  extends DriverSidedTileEntity {
  def getTileEntityClass: Class[_] = classOf[TileEntityExtremeAutoCrafter]

  override def createEnvironment(world: World, x: Int, y: Int, z: Int, side: ForgeDirection): ManagedEnvironment = new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileEntityExtremeAutoCrafter])

  final class Environment(val tile: TileEntityExtremeAutoCrafter) extends ManagedTileEntityEnvironment[TileEntityExtremeAutoCrafter](tile, "extreme_autocrafter") {
    @Callback(doc = "function(slot:number, database:string, database_slot:number):boolean -- Set the ghost item at the specified slot. Item should be in database.")
    def setGhostItem(context: Context, args: Arguments): Array[AnyRef] = {
      val slot = args.checkInteger(0)
      if (slot > 80) throw new IllegalArgumentException("Slot number should be from 0 to 80 (inclusive)")
      tile.setInventorySlotContents(slot + 81, getStack(args))
      result(true)
    }
    @Callback(doc = "function(slot:number):table -- Returns the ghost item at the specified slot.")
    def getGhostItem(context: Context, args: Arguments): Array[AnyRef] = {
      val slot = args.checkInteger(0)
      if (slot > 80) throw new IllegalArgumentException("Slot number should be from 0 to 80 (inclusive)")
      result(tile.getStackInSlot(slot + 81))
    }
    private def getStack(args: Arguments) =
      if (args.count > 1) {
        val (address, entry, size) =
          if (args.isString(0)) (args.checkString(0), args.checkInteger(1), args.optInteger(2, 1))
          else (args.checkString(1), args.checkInteger(2), args.optInteger(3, 1))

        node.network.node(address) match {
          case component: Component => component.host match {
            case database: Database =>
              val dbStack = database.getStackInSlot(entry - 1)
              if (dbStack == null || size < 1) null
              else {
                dbStack.stackSize = math.min(size, dbStack.getMaxStackSize)
                dbStack
              }
            case _ => throw new IllegalArgumentException("not a database")
          }
          case _ => throw new IllegalArgumentException("no such component")
        }
      }
      else null
  }
  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (stack.getItem == Item.getItemFromBlock(BlockExtremeAutoCrafter.instance))
        classOf[Environment]
      else null
    }
  }
}
