package li.cil.oc.server.component

import li.cil.oc.api.network.{Visibility, Message, Node}
import net.minecraftforge.common.ForgeDirection

class RedstoneCard extends Node {

  override def name = "redstone"

  override def visibility = Visibility.Neighbors

  override def receive(message: Message) = super.receive(message).orElse {
    message.data match {
      case Array(target: Array[Byte], side: Double) if message.name == "redstone.input" =>
        network.get.sendToAddress(this, new String(target, "UTF-8"),
          "redstone.input", ForgeDirection.getOrientation(side.toInt))
      case Array(target: Array[Byte], side: Double) if message.name == "redstone.output" =>
        network.get.sendToAddress(this, new String(target, "UTF-8"),
          "redstone.output", ForgeDirection.getOrientation(side.toInt))
      case Array(target: Array[Byte], side: Double, value: Double) if message.name == "redstone.output=" =>
        network.get.sendToAddress(this, new String(target, "UTF-8"),
          "redstone.output=", ForgeDirection.getOrientation(side.toInt), value.toInt)
      case _ => None // Ignore.
    }
  }
}
