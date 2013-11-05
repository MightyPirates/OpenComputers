package li.cil.oc.server.component

import li.cil.oc.Config
import li.cil.oc.api
import li.cil.oc.api.network.{LuaCallback, Context, Arguments, Visibility}
import net.minecraft.tileentity.TileEntityCommandBlock

class CommandBlock(entity: TileEntityCommandBlock) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withComponent("command_block").
    create()

  // ----------------------------------------------------------------------- //

  @LuaCallback("getValue")
  def getValue(context: Context, args: Arguments): Array[AnyRef] = Array(entity.getCommand)

  @LuaCallback("setValue")
  def setValue(context: Context, args: Arguments): Array[AnyRef] = {
    val value = args.checkString(0)
    entity.setCommand(value)
    entity.worldObj.markBlockForUpdate(entity.xCoord, entity.yCoord, entity.zCoord)
    result(true)
  }

  @LuaCallback("run")
  def run(context: Context, args: Arguments): Array[AnyRef] = {
    val name = if (Config.commandUser != null && !Config.commandUser.isEmpty)
      Config.commandUser
    else
      context.address
    entity.setCommandSenderName(name)
    result(entity.executeCommandOnPowered(entity.worldObj) != 0)
  }
}
