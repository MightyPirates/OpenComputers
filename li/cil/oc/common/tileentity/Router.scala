package li.cil.oc.common.tileentity

import li.cil.oc.api
import li.cil.oc.api.network.{Node, Message, Visibility}

class Router extends net.minecraft.tileentity.TileEntity with api.network.Environment {
  val node = api.Network.newNode(this, Visibility.None).create()

  def onMessage(message: Message) {

  }

  def onDisconnect(node: Node) {

  }

  def onConnect(node: Node) {

  }
}
