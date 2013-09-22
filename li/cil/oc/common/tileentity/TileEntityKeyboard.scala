package li.cil.oc.common.tileentity

import cpw.mods.fml.common.network.Player
import li.cil.oc.server.computer.NetworkNode
import li.cil.oc.api.INetworkMessage

class TileEntityKeyboard extends TileEntityRotatable with NetworkNode {
  def id = 0

  override def receive(message: INetworkMessage) = message.getData match {
    case Array(name: String, p: Player, c: Character) if name == "tryKeyDown" => {
      // TODO check if player is close enough
      getNetwork.sendToAll(this, "signal", "keyDown", c)
      message.cancel()
    }
    case Array(name: String, p: Player, c: Character) if name == "tryKeyUp" => {
      // TODO check if player is close enough
      getNetwork.sendToAll(this, "signal", "keyUp", c)
      message.cancel()
    }
    case _ => // Ignore.
  }
}