package li.cil.oc.server.component

import li.cil.oc.api.network.{Component, Visibility, Message}
import net.minecraftforge.common.ForgeDirection

class RedstoneCard extends Component {
  override val name = "redstone"

  override val visibility = Visibility.Neighbors

  componentVisibility = visibility

  override def receive(message: Message) = Option(super.receive(message)).orElse {
    message.data match {
      case Array(side: java.lang.Double) if message.name == "redstone.input" =>
        Option(network.get.sendToAddress(this, message.source.address.get,
          "redstone.input", ForgeDirection.getOrientation(side.toInt)))
      case Array(side: java.lang.Double) if message.name == "redstone.output" =>
        Option(network.get.sendToAddress(this, message.source.address.get,
          "redstone.output", ForgeDirection.getOrientation(side.toInt)))
      case Array(side: java.lang.Double, value: java.lang.Double) if message.name == "redstone.output=" =>
        Option(network.get.sendToAddress(this, message.source.address.get,
          "redstone.output=", ForgeDirection.getOrientation(side.toInt), Int.box(value.toInt)))
      case _ => None // Ignore.
    }
  }.orNull
}
