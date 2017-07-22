package li.cil.oc.integration.nek

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.Side
import li.cil.oc.OpenComputers
import li.cil.oc.client.KeyBindings
import li.cil.oc.integration.ModProxy
import li.cil.oc.integration.Mods
import modwarriors.notenoughkeys.Helper
import modwarriors.notenoughkeys.api.Api
import modwarriors.notenoughkeys.keys.KeyHelper
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import org.lwjgl.input.Keyboard

object ModNotEnoughKeys extends ModProxy {
  override def getMod = Mods.NotEnoughKeys

  override def initialize(): Unit = {
    if (FMLCommonHandler.instance.getSide == Side.CLIENT) {
      Api.registerMod(OpenComputers.ID, KeyBindings.clipboardPaste.getKeyDescription, KeyBindings.materialCosts.getKeyDescription)

      KeyBindings.keyBindingChecks.append(isKeyBindingPressed)
      KeyBindings.keyBindingNameGetters.prepend(getKeyBindingName) // Run before vanilla resolver.
    }
  }

  def isKeyBindingPressed(kb: KeyBinding): Boolean = try {
    Helper.isKeyPressed_KeyBoard(kb) && (Option(KeyHelper.alternates.get(kb.getKeyDescription)) match {
      case Some(Array(shift, ctrl, alt)) =>
        Helper.isShiftKeyDown == shift &&
          Helper.isCtrlKeyDown == ctrl &&
          Helper.isAltKeyDown == alt
      case _ => true
    })
  }
  catch {
    case _: Throwable => true
  }

  def getKeyBindingName(kb: KeyBinding) = try {
    Option(KeyHelper.alternates.get(kb.getKeyDescription)) match {
      case Some(Array(shift, ctrl, alt)) =>
        val baseName = GameSettings.getKeyDisplayString(kb.getKeyCode)
        val modifierNames = Array(
          if (ctrl) GameSettings.getKeyDisplayString(Keyboard.KEY_LCONTROL) else null,
          if (alt) GameSettings.getKeyDisplayString(Keyboard.KEY_LMENU) else null,
          if (shift) GameSettings.getKeyDisplayString(Keyboard.KEY_LSHIFT) else null).
          filter(_ != null).
          mkString("+")
        Some(modifierNames + "+" + baseName)
      case _ => None // Use default.
    }
  }
  catch {
    case _: Throwable => None
  }
}
