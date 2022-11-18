package li.cil.oc.client

import li.cil.oc.OpenComputers
import li.cil.oc.client.gui.traits.InputBuffer
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.client.util.InputMappings
import net.minecraftforge.client.settings.IKeyConflictContext
import net.minecraftforge.client.settings.KeyConflictContext
import net.minecraftforge.client.settings.KeyModifier
import org.lwjgl.glfw.GLFW

import scala.collection.mutable

object KeyBindings {
  private def isActive(input: InputMappings.Input): Boolean = {
    val window = Minecraft.getInstance.getWindow.getWindow
    input.getType match {
      case InputMappings.Type.MOUSE => GLFW.glfwGetMouseButton(window, input.getValue) == GLFW.GLFW_PRESS
      case InputMappings.Type.SCANCODE => false // GLFW doesn't have a glfwGetScancode method to test these.
      case InputMappings.Type.KEYSYM => GLFW.glfwGetKey(window, input.getValue) == GLFW.GLFW_PRESS
    }
  }

  def showExtendedTooltips: Boolean = {
    if (extendedTooltip.isDown) return true
    // We have to know if the keybind is pressed even if the active screen doesn't pass events.
    if (!extendedTooltip.getKeyConflictContext.isActive) return false
    if (extendedTooltip.getKey == InputMappings.UNKNOWN) return false
    extendedTooltip.getKeyModifier match {
      // KeyModifier.NONE does not accept pure modifier keys by default, so check for that.
      case KeyModifier.NONE if KeyModifier.isKeyCodeModifier(extendedTooltip.getKey) => isActive(extendedTooltip.getKey)
      case mod if mod.isActive(extendedTooltip.getKeyConflictContext) => isActive(extendedTooltip.getKey)
      case _ => false
    }
  }

  def isAnalyzeCopyingAddress: Boolean = analyzeCopyAddr.isDown

  def getKeyBindingName(keyBinding: KeyBinding) = keyBinding.getTranslatedKeyMessage.getString

  val textInputConflict = new IKeyConflictContext {
    override def isActive: Boolean = Minecraft.getInstance.screen.isInstanceOf[InputBuffer]

    override def conflicts(other: IKeyConflictContext): Boolean = this == other
  }

  val extendedTooltip = new KeyBinding("key.opencomputers.extendedTooltip", KeyConflictContext.GUI,
    InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, OpenComputers.Name)

  val analyzeCopyAddr = new KeyBinding("key.opencomputers.analyzeCopyAddress", KeyConflictContext.IN_GAME,
    InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL, OpenComputers.Name)

  val clipboardPaste = new KeyBinding("key.opencomputers.clipboardPaste", textInputConflict,
    InputMappings.Type.KEYSYM, GLFW.GLFW_KEY_INSERT, OpenComputers.Name)
}
