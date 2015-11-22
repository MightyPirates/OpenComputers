package li.cil.oc.integration.vanilla

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityCommandBlock
import net.minecraft.world.World

object DriverCommandBlock extends DriverTileEntity {
  override def getTileEntityClass: Class[_] = classOf[TileEntityCommandBlock]

  override def createEnvironment(world: World, x: Int, y: Int, z: Int): ManagedEnvironment =
    new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileEntityCommandBlock])

  final class Environment(tileEntity: TileEntityCommandBlock) extends ManagedTileEntityEnvironment[TileEntityCommandBlock](tileEntity, "command_block") with NamedBlock {
    override def preferredName = "command_block"

    override def priority = 0

    @Callback(direct = true, doc = "function():string -- Get the command currently set in this command block.")
    def getCommand(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.func_145993_a.func_145753_i)
    }

    @Callback(doc = "function(value:string) -- Set the specified command for the command block.")
    def setCommand(context: Context, args: Arguments): Array[AnyRef] = {
      tileEntity.func_145993_a.func_145752_a(args.checkString(0))
      tileEntity.getWorldObj.markBlockForUpdate(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord)
      result(true)
    }

    @Callback(doc = "function():number -- Execute the currently set command. This has a slight delay to allow the command block to properly update.")
    def executeCommand(context: Context, args: Arguments): Array[AnyRef] = {
      context.pause(0.1)
      val commandSender = tileEntity.func_145993_a
      commandSender.func_145755_a(tileEntity.getWorldObj)
      result(commandSender.func_145760_g, commandSender.func_145749_h.getUnformattedText)
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (stack != null && Block.getBlockFromItem(stack.getItem) == Blocks.command_block)
        classOf[Environment]
      else null
    }
  }

}
