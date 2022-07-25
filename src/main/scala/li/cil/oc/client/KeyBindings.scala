package li.cil.oc.client

import li.cil.oc.OpenComputers
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.client.util.InputMappings
import net.minecraftforge.client.settings.KeyConflictContext
import org.lwjgl.glfw.GLFW

import scala.collection.mutable

object KeyBindings {
  def showExtendedTooltips: Boolean = extendedTooltip.isDown

  def isAnalyzeCopyingAddress: Boolean = analyzeCopyAddr.isDown

  def getKeyBindingName(keyBinding: KeyBinding) = keyBinding.getTranslatedKeyMessage.getString

  val extendedTooltip = Minecraft.getInstance.options.keyShift

  val analyzeCopyAddr = new KeyBinding("key.opencomputers.analyzeCopyAddress", KeyConflictContext.IN_GAME,
    InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, OpenComputers.Name)

  val clipboardPaste = new KeyBinding("key.opencomputers.clipboardPaste", KeyConflictContext.GUI,
    InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_INSERT, OpenComputers.Name)
}
