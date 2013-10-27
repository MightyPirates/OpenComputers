package li.cil.oc.server.component

import li.cil.oc.Config
import li.cil.oc.api
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.network.environment.{Context, Arguments, LuaCallback}
import net.minecraft.tileentity.TileEntityCommandBlock

class CommandBlock(entity: TileEntityCommandBlock) extends ManagedComponent {
  val node = api.Network.createComponent(api.Network.createNode(this, "command_block", Visibility.Network))

  // ----------------------------------------------------------------------- //

  @LuaCallback("getValue")
  def getValue(context: Context, args: Arguments): Array[Object] = Array(entity.getCommand)

  @LuaCallback("setValue")
  def setValue(context: Context, args: Arguments): Array[Object] = {
    val value = args.checkString(1)
    entity.setCommand(value)
    entity.worldObj.markBlockForUpdate(entity.xCoord, entity.yCoord, entity.zCoord)
    result(true)
  }

  @LuaCallback("run")
  def run(context: Context, args: Arguments): Array[Object] = {
    val name = if (Config.commandUser != null && !Config.commandUser.isEmpty)
      Config.commandUser
    else
      context.address
    entity.setCommandSenderName(name)
    result(entity.executeCommandOnPowered(entity.worldObj) != 0)
  }
}
