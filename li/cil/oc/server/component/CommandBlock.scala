package li.cil.oc.server.component

import li.cil.oc.api.network.{ComputerVisible, Message, Visibility}
import net.minecraft.tileentity.TileEntityCommandBlock

class CommandBlock(entity: TileEntityCommandBlock) extends ComputerVisible {
  val name = "command_block"

  val visibility = Visibility.Network

  computerVisibility = visibility

  override def receive(message: Message) = super.receive(message).orElse {
    message.data match {
      case Array() if message.name == "command.value" =>
        result(entity.getCommand)
      case Array(value: Array[Byte]) if message.name == "command.value=" =>
        entity.setCommand(new String(value, "UTF-8"))
        entity.worldObj.markBlockForUpdate(entity.xCoord, entity.yCoord, entity.zCoord)
        result(true)
      case Array() if message.name == "command.run" =>
        entity.setCommandSenderName(message.source.address.get)
        result(entity.executeCommandOnPowered(entity.worldObj) != 0)
      case _ => None
    }
  }
}
