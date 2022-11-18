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
import net.minecraft.block.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.CommandBlockTileEntity
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.server.ServerLifecycleHooks

object DriverCommandBlock extends DriverSidedTileEntity {
  override def getTileEntityClass: Class[_] = classOf[CommandBlockTileEntity]

  override def createEnvironment(world: World, pos: BlockPos, side: Direction): ManagedEnvironment =
    new Environment(world.getBlockEntity(pos).asInstanceOf[CommandBlockTileEntity])

  final class Environment(tileEntity: CommandBlockTileEntity) extends ManagedTileEntityEnvironment[CommandBlockTileEntity](tileEntity, "command_block") with NamedBlock {
    override def preferredName = "command_block"

    override def priority = 0

    @Callback(direct = true, doc = "function():string -- Get the command currently set in this command block.")
    def getCommand(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getCommandBlock.getCommand)
    }

    @Callback(doc = "function(value:string) -- Set the specified command for the command block.")
    def setCommand(context: Context, args: Arguments): Array[AnyRef] = {
      tileEntity.getCommandBlock.setCommand(args.checkString(0))
      tileEntity.getLevel.sendBlockUpdated(tileEntity.getBlockPos, tileEntity.getLevel.getBlockState(tileEntity.getBlockPos), tileEntity.getLevel.getBlockState(tileEntity.getBlockPos), 3)
      result(true)
    }

    @Callback(doc = "function():number -- Execute the currently set command. This has a slight delay to allow the command block to properly update.")
    def executeCommand(context: Context, args: Arguments): Array[AnyRef] = {
      context.pause(0.1)
      if (!ServerLifecycleHooks.getCurrentServer.isCommandBlockEnabled) {
        result(null, "command blocks are disabled")
      } else {
        val commandSender = tileEntity.getCommandBlock
        commandSender.performCommand(tileEntity.getLevel)
        result(commandSender.getSuccessCount, commandSender.getLastOutput.getString)
      }
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (!stack.isEmpty && Block.byItem(stack.getItem) == Blocks.COMMAND_BLOCK)
        classOf[Environment]
      else null
    }
  }

}
