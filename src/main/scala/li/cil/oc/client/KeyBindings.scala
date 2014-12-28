package li.cil.oc.client

import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.client.FMLClientHandler
import org.lwjgl.input.Keyboard

object KeyBindings {
  def showExtendedTooltips = Keyboard.isCreated && Keyboard.isKeyDown(extendedTooltip.getKeyCode)

  def showMaterialCosts = Keyboard.isCreated && Keyboard.isKeyDown(materialCosts.getKeyCode)

  def extendedTooltip = FMLClientHandler.instance.getClient.gameSettings.keyBindSneak

  val materialCosts = new KeyBinding("key.materialCosts", Keyboard.KEY_LMENU, "OpenComputers")

  val clipboardPaste = new KeyBinding("key.clipboardPaste", Keyboard.KEY_INSERT, "OpenComputers")
}
