package li.cil.oc.client

import net.minecraftforge.fml.client.CustomModLoadingErrorDisplayException
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiErrorScreen

class OpenComponentsPresentException extends CustomModLoadingErrorDisplayException {
  private val message = "The functionality OpenComponents provided for previous versions of OpenComputers has been integrated into OpenComputers 1.4.\n\nPlease remove OpenComponents, as it will otherwise lead to incompatibilities and crashes."

  override def initGui(errorScreen: GuiErrorScreen, fontRenderer: FontRenderer) {
  }

  override def drawScreen(errorScreen: GuiErrorScreen, fontRenderer: FontRenderer, mouseRelX: Int, mouseRelY: Int, tickTime: Float) {
    fontRenderer.drawStringWithShadow("§lWARNING", (errorScreen.width - fontRenderer.getStringWidth("WARNING")) / 2, 30, 0xFF0000)
    fontRenderer.drawSplitString(message, 20, 50, errorScreen.width - 40, 0xFFFFFF)
  }
}
