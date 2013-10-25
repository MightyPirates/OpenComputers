package li.cil.oc.server.component

import li.cil.oc.Config
import li.cil.oc.api.network.{Component, Message, Visibility}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntityCommandBlock

class CommandBlock(entity: TileEntityCommandBlock) extends Component {
  val name = "command_block"

  val visibility = Visibility.Network

  componentVisibility = visibility

  override def receive(message: Message) = super.receive(message).orElse {
    message.data match {
      case Array() if message.name == "command.value" =>
        result(entity.getCommand)
      case Array(value: Array[Byte]) if message.name == "command.value=" =>
        entity.setCommand(new String(value, "UTF-8"))
        entity.worldObj.markBlockForUpdate(entity.xCoord, entity.yCoord, entity.zCoord)
        result(true)
      case Array() if message.name == "command.run" =>
        val name = if (Config.commandUser != null && !Config.commandUser.trim.isEmpty)
          Config.commandUser.trim
        else
          message.source.address.get
        entity.setCommandSenderName(name)
        result(entity.executeCommandOnPowered(entity.worldObj) != 0)
      case _ => None
    }
  }
}
