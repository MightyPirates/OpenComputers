package li.cil.oc.integration.appeng

import appeng.api.AEApi
import appeng.tile.misc.TileInterface
import cpw.mods.fml.common.versioning.VersionRange
import cpw.mods.fml.common.Loader
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.driver.EnvironmentAware
import li.cil.oc.api.internal.Database
import li.cil.oc.api.prefab.DriverTileEntity
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.network.Component
import li.cil.oc.integration.Mods
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ExtendedArguments._
import li.cil.oc.util.ResultWrapper._
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection



object DriverBlockInterface extends DriverTileEntity with EnvironmentAware {
  def getTileEntityClass: Class[_] = classOf[TileInterface]
  
  def createEnvironment(world: World, x: Int, y: Int, z: Int): ManagedEnvironment =
    new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileInterface])
  
  override def providedEnvironment(stack: ItemStack) = 
    if (stack != null &&
        AEApi.instance != null &&
      AEApi.instance.blocks != null &&
      AEApi.instance.blocks.blockInterface != null &&
      Block.getBlockFromItem(stack.getItem) == AEApi.instance().blocks().blockInterface.block) classOf[Environment]
    else null
  
  class Environment(tileEntity: TileInterface) extends ManagedTileEntityEnvironment[TileInterface](tileEntity, "me_interface") with NamedBlock {
    override def preferredName = "me_interface"

    override def priority = 1
      
    @Callback(doc = "function([slot:number]):table -- Get the configuration of the interface.")
    def getInterfaceConfiguration(context: Context, args: Arguments): Array[AnyRef] = {
      val config = tileEntity.getInventoryByName("config")
      val slot = args.optSlot(config, 2, 0)
      val stack = config.getStackInSlot(slot)
      result(stack)
    }
    
    @Callback(doc = "function([slot:number][, database:address, entry:number, size:number]):boolean -- Configure the interface pointing in the specified direction..")
    def setInterfaceConfiguration(context: Context, args: Arguments): Array[AnyRef] = {

      val config = tileEntity.getInventoryByName("config")
      val slot = if (args.count > 3 || args.count < 3) args.optSlot(config, 0, 0) else 0
      val stack = if (args.count > 1) {
        val (address, entry, size) =
          if (args.count > 3) (args.checkString(1), args.checkInteger(2), args.checkInteger(3))
          else (args.checkString(0), args.checkInteger(1), args.checkInteger(2))
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
    }
  }
}