package li.cil.oc.server.component

import cpw.mods.fml.common.IPlayerTracker
import li.cil.oc.api.Network
import li.cil.oc.api.network.{Node, Visibility, Message}
import li.cil.oc.common.tileentity.Environment
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.{Event, ForgeSubscribe}
import scala.collection.mutable

// TODO key up when screen is disconnected from which the key down came
// TODO key up after load for anything that was pressed

class Keyboard(owner: Environment) extends ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
    withComponent("keyboard").
    create()

  val pressedKeys = mutable.Map.empty[EntityPlayer, mutable.Map[Integer, Character]]

  // ----------------------------------------------------------------------- //

  @ForgeSubscribe
  def onReleasePressedKeys(e: Keyboard.ReleasePressedKeys) {
    pressedKeys.get(e.player) match {
      case Some(keys) => for ((code, char) <- keys)
        owner.node.sendToReachable("computer.checked_signal", e.player, "key_up", char, code, e.player.getCommandSenderName)
      case _ =>
    }
    pressedKeys.remove(e.player)
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    if (node == this.node) {
      MinecraftForge.EVENT_BUS.register(this)
    }
  }

  override def onDisconnect(node: Node) {
    if (node == this.node) {
      MinecraftForge.EVENT_BUS.unregister(this)
    }
  }

  override def onMessage(message: Message) = {
    message.data match {
      case Array(p: EntityPlayer, char: Character, code: Integer) if message.name == "keyboard.keyDown" =>
        if (isUseableByPlayer(p)) {
          pressedKeys.getOrElseUpdate(p, mutable.Map.empty[Integer, Character]) += code -> char
          node.sendToReachable("computer.checked_signal", p, "key_down", char, code, p.getCommandSenderName)
        }
      case Array(p: EntityPlayer, char: Character, code: Integer) if message.name == "keyboard.keyUp" =>
        pressedKeys.get(p) match {
          case Some(keys) if keys.contains(code) =>
            keys -= code
            node.sendToReachable("computer.checked_signal", p, "key_up", char, code, p.getCommandSenderName)
          case _ =>
        }
      case Array(p: EntityPlayer, value: String) if message.name == "keyboard.clipboard" =>
        if (isUseableByPlayer(p)) {
          node.sendToReachable("computer.checked_signal", p, "clipboard", value, p.getCommandSenderName)
        }
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  def isUseableByPlayer(p: EntityPlayer) =
    owner.world.getBlockTileEntity(owner.x, owner.y, owner.z) == owner &&
      p.getDistanceSq(owner.x + 0.5, owner.y + 0.5, owner.z + 0.5) <= 64
}

object Keyboard extends IPlayerTracker {

  def onPlayerRespawn(player: EntityPlayer) {
    MinecraftForge.EVENT_BUS.post(new ReleasePressedKeys(player))
  }

  def onPlayerChangedDimension(player: EntityPlayer) {
    MinecraftForge.EVENT_BUS.post(new ReleasePressedKeys(player))
  }

  def onPlayerLogout(player: EntityPlayer) {
    MinecraftForge.EVENT_BUS.post(new ReleasePressedKeys(player))
  }

  def onPlayerLogin(player: EntityPlayer) {}

  class ReleasePressedKeys(val player: EntityPlayer) extends Event

}
