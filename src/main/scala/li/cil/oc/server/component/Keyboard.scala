package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.internal.Keyboard.UsabilityChecker
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Message
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import net.minecraft.entity.player.PlayerEntity

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.mutable

// TODO key up when screen is disconnected from which the key down came
// TODO key up after load for anything that was pressed

class Keyboard(val host: EnvironmentHost) extends AbstractManagedEnvironment with api.internal.Keyboard with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("keyboard").
    create()

  val pressedKeys = mutable.Map.empty[PlayerEntity, mutable.Map[Integer, Character]]

  var usableOverride: Option[api.internal.Keyboard.UsabilityChecker] = None

  override def setUsableOverride(callback: UsabilityChecker) = usableOverride = Option(callback)

  // ----------------------------------------------------------------------- //

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Input,
    DeviceAttribute.Description -> "Keyboard",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Fancytyper MX-Stone"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  def releasePressedKeys(player: PlayerEntity) {
    pressedKeys.get(player) match {
      case Some(keys) => for ((code, char) <- keys) {
        if (Settings.get.inputUsername) {
          signal(player, "key_up", char, code, player.getName.getString)
        }
        else {
          signal(player, "key_up", char, code)
        }
      }
      case _ =>
    }
    pressedKeys.remove(player)
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: Message) = {
    message.data match {
      case Array(p: PlayerEntity, char: Character, code: Integer) if message.name == "keyboard.keyDown" =>
        if (isUsableByPlayer(p)) {
          pressedKeys.getOrElseUpdate(p, mutable.Map.empty[Integer, Character]) += code -> char
          if (Settings.get.inputUsername) {
            signal(p, "key_down", char, code, p.getName.getString)
          }
          else {
            signal(p, "key_down", char, code)
          }
        }
      case Array(p: PlayerEntity, char: Character, code: Integer) if message.name == "keyboard.keyUp" =>
        pressedKeys.get(p) match {
          case Some(keys) if keys.contains(code) =>
            keys -= code
            if (Settings.get.inputUsername) {
              signal(p, "key_up", char, code, p.getName.getString)
            }
            else {
              signal(p, "key_up", char, code)
            }
          case _ =>
        }
      case Array(p: PlayerEntity, value: String) if message.name == "keyboard.clipboard" =>
        if (isUsableByPlayer(p)) {
          for (line <- value.linesWithSeparators) {
            if (Settings.get.inputUsername) {
              signal(p, "clipboard", line, p.getName.getString)
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

  def isUsableByPlayer(p: PlayerEntity) = usableOverride match {
    case Some(callback) => callback.isUsableByPlayer(this, p)
    case _ => p.distanceToSqr(host.xPosition, host.yPosition, host.zPosition) <= 64
  }

  protected def signal(args: AnyRef*) =
    node.sendToReachable("computer.checked_signal", args: _*)
}
