package li.cil.oc.client

import li.cil.oc.OpenComputers
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.client.FMLClientHandler
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

object KeyBindings {
  def showExtendedTooltips = isKeybindPressed(extendedTooltip)

  def showMaterialCosts = isKeybindPressed(materialCosts)

  def isPastingClipboard = isKeybindPressed(clipboardPaste)

  def getKeybindName(keyBinding: KeyBinding) = try {
    if (keyBinding.getKeyCode < 0)
      Mouse.getButtonName(keyBinding.getKeyCode + 100)
    else
      Keyboard.getKeyName(keyBinding.getKeyCode)
  }
  catch {
    case _: Throwable => "???"
  }

  def isKeybindPressed(keyBinding: KeyBinding) = try {
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
