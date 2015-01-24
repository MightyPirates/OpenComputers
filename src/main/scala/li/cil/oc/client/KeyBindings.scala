package li.cil.oc.client

import cpw.mods.fml.client.FMLClientHandler
import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard

object KeyBindings {
  def showExtendedTooltips = Keyboard.isCreated && (try Keyboard.isKeyDown(extendedTooltip.getKeyCode) catch {
    case _: Throwable => false // Don't ask me, sometimes things can apparently screw up LWJGL's keyboard handling.
  })

  def showMaterialCosts = Keyboard.isCreated && (try Keyboard.isKeyDown(materialCosts.getKeyCode) catch {
    case _: Throwable => false // Don't ask me, sometimes things can apparently screw up LWJGL's keyboard handling.
  })

  def extendedTooltip = FMLClientHandler.instance.getClient.gameSettings.keyBindSneak

  val materialCosts = new KeyBinding("key.materialCosts", Keyboard.KEY_LMENU, "OpenComputers")

  val clipboardPaste = new KeyBinding("key.clipboardPaste", Keyboard.KEY_INSERT, "OpenComputers")
}
