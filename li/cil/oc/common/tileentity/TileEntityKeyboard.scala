package li.cil.oc.common.tileentity

import cpw.mods.fml.common.network.Player
import li.cil.oc.api.{INetworkNode, INetworkMessage}

class TileEntityKeyboard extends TileEntityRotatable with INetworkNode {
  override def name = "keyboard"

  override def receive(message: INetworkMessage) = {
    super.receive(message)
    message.data match {
      case Array(p: Player, char: Char, code: Int) if message.name == "keyboard.keyDown" => {
        // TODO check if player is close enough and only consume message if so
        network.sendToAll(this, "signal", "key_down", char.toString, code)
        message.cancel() // One keyboard is enough.
        None
      }
      case Array(p: Player, char: Char, code: Int) if message.name == "keyboard.keyUp" => {
        // TODO check if player is close enough and only consume message if so
        network.sendToAll(this, "signal", "key_up", char.toString, code)
        message.cancel() // One keyboard is enough.
        None
      }
      case _ => None
    }
  }
}