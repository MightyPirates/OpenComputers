package li.cil.oc.server.component

import li.cil.oc.Config
import li.cil.oc.api
import li.cil.oc.api.network.environment.LuaCallback
import li.cil.oc.api.network.{Message, Visibility}
import net.minecraft.tileentity.TileEntityCommandBlock

class CommandBlock(entity: TileEntityCommandBlock) extends ManagedComponent {
  val node = api.Network.createComponent(api.Network.createNode(this, "command_block", Visibility.Network))

  // ----------------------------------------------------------------------- //

  @LuaCallback("getValue")
  def getValue(message: Message): Array[Object] = Array(entity.getCommand)

  @LuaCallback("setValue")
  def setValue(message: Message): Array[Object] = {
    val value = message.checkString(1)
    entity.setCommand(value)
    entity.worldObj.markBlockForUpdate(entity.xCoord, entity.yCoord, entity.zCoord)
    result(true)
  }

  @LuaCallback("run")
  def run(message: Message): Array[Object] = {
    val name = if (Config.commandUser != null && !Config.commandUser.isEmpty)
      Config.commandUser
    else
      message.source.address
    entity.setCommandSenderName(name)
    result(entity.executeCommandOnPowered(entity.worldObj) != 0)
  }
}
