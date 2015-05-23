package li.cil.oc.client

import cpw.mods.fml.client.FMLClientHandler
import li.cil.oc.OpenComputers
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

import scala.collection.mutable

object KeyBindings {
  val keyBindingChecks = mutable.ArrayBuffer(isKeyBindingPressedVanilla _)

  val keyBindingNameGetters = mutable.ArrayBuffer(getKeyBindingNameVanilla _)

  def showExtendedTooltips = isKeyBindingPressed(extendedTooltip)

  def showMaterialCosts = isKeyBindingPressed(materialCosts)

  def isPastingClipboard = isKeyBindingPressed(clipboardPaste)

  def getKeyBindingName(keyBinding: KeyBinding) = keyBindingNameGetters.map(_(keyBinding)).collectFirst {
    case Some(name) => name
  }.getOrElse("???")

  def isKeyBindingPressed(keyBinding: KeyBinding) = keyBindingChecks.forall(_(keyBinding))

  def getKeyBindingNameVanilla(keyBinding: KeyBinding) = try Some(GameSettings.getKeyDisplayString(keyBinding.getKeyCode)) catch {
    case _: Throwable => None
  }

  def isKeyBindingPressedVanilla(keyBinding: KeyBinding) = try {
    if (keyBinding.getKeyCode < 0)
      Mouse.isCreated && Mouse.isButtonDown(keyBinding.getKeyCode + 100)
    else
      Keyboard.isCreated && Keyboard.isKeyDown(keyBinding.getKeyCode)
  }
  catch {
    case _: Throwable => false
  }

  def extendedTooltip = FMLClientHandler.instance.getClient.gameSettings.keyBindSneak

  val materialCosts = new KeyBinding("key.materialCosts", Keyboard.KEY_LMENU, OpenComputers.Name)

  val clipboardPaste = new KeyBinding("key.clipboardPaste", Keyboard.KEY_INSERT, OpenComputers.Name)
}
