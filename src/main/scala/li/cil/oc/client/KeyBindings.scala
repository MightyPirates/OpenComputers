package li.cil.oc.client

import li.cil.oc.OpenComputers
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.client.FMLClientHandler
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

import scala.collection.mutable

object KeyBindings {
  val keyBindingChecks = mutable.ArrayBuffer(isKeyBindingPressedVanilla _)

  val keyBindingNameGetters = mutable.ArrayBuffer(getKeyBindingNameVanilla _)

  def showExtendedTooltips: Boolean = isKeyBindingPressed(extendedTooltip)

  def isPastingClipboard: Boolean = isKeyBindingPressed(clipboardPaste)

  def getKeyBindingName(keyBinding: KeyBinding): String = keyBindingNameGetters.map(_ (keyBinding)).collectFirst {
    case Some(name) => name
  }.getOrElse("???")

  def isKeyBindingPressed(keyBinding: KeyBinding): Boolean = keyBindingChecks.forall(_ (keyBinding))

  def getKeyBindingNameVanilla(keyBinding: KeyBinding): Option[String] = try Some(GameSettings.getKeyDisplayString(keyBinding.getKeyCode)) catch {
    case _: Throwable => None
  }

  def isKeyBindingPressedVanilla(keyBinding: KeyBinding): Boolean = try {
    if (keyBinding.getKeyCode < 0)
      Mouse.isCreated && Mouse.isButtonDown(keyBinding.getKeyCode + 100)
    else
      Keyboard.isCreated && Keyboard.isKeyDown(keyBinding.getKeyCode)
  }
  catch {
    case _: Throwable => false
  }

  def extendedTooltip: KeyBinding = FMLClientHandler.instance.getClient.gameSettings.keyBindSneak

  val clipboardPaste = new KeyBinding("key.clipboardPaste", Keyboard.KEY_INSERT, OpenComputers.Name)
}
