package li.cil.oc.integration.minecraft

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityCommandBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.FMLCommonHandler

object DriverCommandBlock extends DriverSidedTileEntity {
  override def getTileEntityClass: Class[_] = classOf[TileEntityCommandBlock]

  override def createEnvironment(world: World, pos: BlockPos, side: EnumFacing): ManagedEnvironment =
    new Environment(world.getTileEntity(pos).asInstanceOf[TileEntityCommandBlock])

  final class Environment(tileEntity: TileEntityCommandBlock) extends ManagedTileEntityEnvironment[TileEntityCommandBlock](tileEntity, "command_block") with NamedBlock {
    override def preferredName = "command_block"

    override def priority = 0

    @Callback(direct = true, doc = "function():string -- Get the command currently set in this command block.")
    def getCommand(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getCommandBlockLogic.getCommand)
    }

    @Callback(doc = "function(value:string) -- Set the specified command for the command block.")
    def setCommand(context: Context, args: Arguments): Array[AnyRef] = {
      tileEntity.getCommandBlockLogic.setCommand(args.checkString(0))
      tileEntity.getWorld.notifyBlockUpdate(tileEntity.getPos, tileEntity.getWorld.getBlockState(tileEntity.getPos), tileEntity.getWorld.getBlockState(tileEntity.getPos), 3)
      result(true)
    }

    @Callback(doc = "function():number -- Execute the currently set command. This has a slight delay to allow the command block to properly update.")
    def executeCommand(context: Context, args: Arguments): Array[AnyRef] = {
      context.pause(0.1)
      if (!FMLCommonHandler.instance.getMinecraftServerInstance.isCommandBlockEnabled) {
        result(null, "command blocks are disabled")
      } else {
        val commandSender = tileEntity.getCommandBlockLogic
        commandSender.trigger(tileEntity.getWorld)
        result(commandSender.getSuccessCount, commandSender.getLastOutput.getUnformattedText)
      }
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (stack != null && Block.getBlockFromItem(stack.getItem) == Blocks.COMMAND_BLOCK)
        classOf[Environment]
      else null
    }
  }

}
