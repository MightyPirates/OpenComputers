package li.cil.oc.client

import java.util

import cpw.mods.fml.client.FMLClientHandler
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler
import cpw.mods.fml.common.TickType
import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard

object KeyBindings {
  def showExtendedTooltips = Keyboard.isCreated && Keyboard.isKeyDown(extendedTooltip.keyCode)

  def showMaterialCosts = Keyboard.isCreated && Keyboard.isKeyDown(materialCosts.keyCode)

  def extendedTooltip = FMLClientHandler.instance.getClient.gameSettings.keyBindSneak

  val materialCosts = new KeyBinding("key.materialCosts", Keyboard.KEY_LMENU)

  val clipboardPaste = new KeyBinding("key.clipboardPaste", Keyboard.KEY_INSERT)

  object Handler extends KeyHandler(Array(
    materialCosts,
    clipboardPaste
  ), Array(
    false,
    false
  )) {
    override def getLabel = "OpenComputers Keys"

    override def ticks() = util.EnumSet.of(TickType.CLIENT)

    override def keyUp(types: util.EnumSet[TickType], kb: KeyBinding, tickEnd: Boolean) {}

    override def keyDown(types: util.EnumSet[TickType], kb: KeyBinding, tickEnd: Boolean, isRepeat: Boolean) {}
  }

}
