package li.cil.oc.server.component

import li.cil.oc.api
import li.cil.oc.api.network.environment.LuaCallback
import li.cil.oc.api.network.{Visibility, Message}
import net.minecraftforge.common.ForgeDirection

class RedstoneCard extends ManagedComponent {
  val node = api.Network.createComponent(api.Network.createNode(this, "redstone", Visibility.Neighbors))

  @LuaCallback("getInput")
  def getInput(message: Message): Array[Object] = {
    val side = message.checkInteger(1)
    node.network.sendToAddress(node, message.source.address,
      "redstone.input", ForgeDirection.getOrientation(side))
  }

  @LuaCallback("getOutput")
  def getOutput(message: Message): Array[Object] = {
    val side = message.checkInteger(1)
    node.network.sendToAddress(node, message.source.address,
      "redstone.output", ForgeDirection.getOrientation(side))
  }

  @LuaCallback("setOutput")
  def setOutput(message: Message): Array[Object] = {
    val side = message.checkInteger(1)
    val value = message.checkInteger(2)
    node.network.sendToAddress(node, message.source.address,
      "redstone.output=", ForgeDirection.getOrientation(side.toInt), Int.box(value))
  }
}
