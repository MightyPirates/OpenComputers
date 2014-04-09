package li.cil.oc.client

import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard

object KeyBindings {
  def showExtendedTooltips = Keyboard.isKeyDown(extendedTooltip.getKeyCode)

  def showMaterialCosts = Keyboard.isKeyDown(materialCosts.getKeyCode)

  val extendedTooltip = new KeyBinding("key.extendedTooltip", Keyboard.KEY_LSHIFT, "OpenComputers")

  val materialCosts = new KeyBinding("key.materialCosts", Keyboard.KEY_LMENU, "OpenComputers")

  val clipboardPaste = new KeyBinding("key.clipboardPaste", Keyboard.KEY_INSERT, "OpenComputers")
}
