package li.cil.oc.server.component

import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.component.Keyboard.UsabilityChecker
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.event.world.WorldEvent

import scala.collection.mutable

// TODO key up when screen is disconnected from which the key down came
// TODO key up after load for anything that was pressed

class Keyboard(val host: EnvironmentHost) extends prefab.ManagedEnvironment with api.component.Keyboard {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("keyboard").
    create()

  val pressedKeys = mutable.Map.empty[EntityPlayer, mutable.Map[Integer, Character]]

  var usableOverride: Option[api.component.Keyboard.UsabilityChecker] = None

  override def setUsableOverride(callback: UsabilityChecker) = usableOverride = Option(callback)

  // ----------------------------------------------------------------------- //

  @SubscribeEvent
  def onPlayerRespawn(e: PlayerRespawnEvent) {
    releasePressedKeys(e.player)
  }

  @SubscribeEvent
  def onPlayerChangedDimension(e: PlayerChangedDimensionEvent) {
    releasePressedKeys(e.player)
  }

  @SubscribeEvent
  def onPlayerLogout(e: PlayerLoggedOutEvent) {
    releasePressedKeys(e.player)
  }

  def releasePressedKeys(player: EntityPlayer) {
    pressedKeys.get(player) match {
      case Some(keys) => for ((code, char) <- keys) {
        if (Settings.get.inputUsername) {
          signal(player, "key_up", char, code, player.getName)
        }
        else {
          signal(player, "key_up", char, code)
        }
      }
      case _ =>
    }
    pressedKeys.remove(player)
  }

  @SubscribeEvent
  def onWorldUnload(e: WorldEvent.Unload) {
    try FMLCommonHandler.instance.bus.unregister(this) catch {
      case ignore: Throwable =>
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    if (node == this.node) {
      FMLCommonHandler.instance.bus.register(this)
    }
  }

  override def onDisconnect(node: Node) {
    if (node == this.node) {
      try FMLCommonHandler.instance.bus.unregister(this) catch {
        case ignore: Throwable =>
      }
    }
  }

  override def onMessage(message: Message) = {
    message.data match {
      case Array(p: EntityPlayer, char: Character, code: Integer) if message.name == "keyboard.keyDown" =>
        if (isUseableByPlayer(p)) {
          pressedKeys.getOrElseUpdate(p, mutable.Map.empty[Integer, Character]) += code -> char
          if (Settings.get.inputUsername) {
            signal(p, "key_down", char, code, p.getName)
          }
          else {
            signal(p, "key_down", char, code)
          }
        }
      case Array(p: EntityPlayer, char: Character, code: Integer) if message.name == "keyboard.keyUp" =>
        pressedKeys.get(p) match {
          case Some(keys) if keys.contains(code) =>
            keys -= code
            if (Settings.get.inputUsername) {
              signal(p, "key_up", char, code, p.getName)
            }
            else {
              signal(p, "key_up", char, code)
            }
          case _ =>
        }
      case Array(p: EntityPlayer, value: String) if message.name == "keyboard.clipboard" =>
        if (isUseableByPlayer(p)) {
          for (line <- value.linesWithSeparators) {
            if (Settings.get.inputUsername) {
              signal(p, "clipboard", line, p.getName)
            }
            else {
              signal(p, "clipboard", line)
            }
          }
        }
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  def isUseableByPlayer(p: EntityPlayer) = usableOverride match {
    case Some(callback) => callback.isUsableByPlayer(this, p)
    case _ => p.getDistanceSq(host.xPosition, host.yPosition, host.zPosition) <= 64
  }

  protected def signal(args: AnyRef*) =
    node.sendToReachable("computer.checked_signal", args: _*)
}
