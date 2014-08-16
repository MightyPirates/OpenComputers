package li.cil.oc.server.component

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.PlayerEvent.{PlayerChangedDimensionEvent, PlayerLoggedOutEvent, PlayerRespawnEvent}
import li.cil.oc.api.Network
import li.cil.oc.api.component.Keyboard.UsabilityChecker
import li.cil.oc.api.driver.Container
import li.cil.oc.api.network.{Message, Node, Visibility}
import li.cil.oc.common.component
import li.cil.oc.{Settings, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent

import scala.collection.mutable

// TODO key up when screen is disconnected from which the key down came
// TODO key up after load for anything that was pressed

class Keyboard(val owner: Container) extends component.ManagedComponent with api.component.Keyboard {
  val node = Network.newNode(this, Visibility.Network).
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
          signal(player, "key_up", char, code, player.getCommandSenderName)
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
    try MinecraftForge.EVENT_BUS.unregister(this) catch {
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
      FMLCommonHandler.instance.bus.unregister(this)
    }
  }

  override def onMessage(message: Message) = {
    message.data match {
      case Array(p: EntityPlayer, char: Character, code: Integer) if message.name == "keyboard.keyDown" =>
        if (isUseableByPlayer(p)) {
          pressedKeys.getOrElseUpdate(p, mutable.Map.empty[Integer, Character]) += code -> char
          if (Settings.get.inputUsername) {
            signal(p, "key_down", char, code, p.getCommandSenderName)
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
              signal(p, "key_up", char, code, p.getCommandSenderName)
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
              signal(p, "clipboard", line, p.getCommandSenderName)
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
    case _ => p.getDistanceSq(owner.xPosition, owner.yPosition, owner.zPosition) <= 64
  }

  protected def signal(args: AnyRef*) =
    node.sendToReachable("computer.checked_signal", args: _*)
}
