package li.cil.oc.client.gui

import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.client.gui.Gui

class GuiScreen(val tileEntity: TileEntityScreen) extends net.minecraft.client.gui.GuiScreen {
  tileEntity.gui = Some(this)

  private var (x, y, bufferWidth, bufferHeight) = (0, 0, 0, 0)
  var lines = Array.empty[String]

  def setSize(w: Int, h: Int) = {
    bufferWidth = w * 5 + 4
    bufferHeight = h * fontRenderer.FONT_HEIGHT + 4
    x = (width - bufferWidth) / 2
    y = (height - bufferHeight) / 2
  }

  override def initGui() = {
    super.initGui()
    val (w, h) = tileEntity.component.resolution
    setSize(w, h)
    lines = tileEntity.component.lines
  }

  override def onGuiClosed = {
    super.onGuiClosed()
    tileEntity.gui = None
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.drawScreen(mouseX, mouseY, dt);
    val padding = 2
    Gui.drawRect(x, y, x + bufferWidth, y + bufferHeight, 0xFF000000)

    val currentX = x + padding
    var currentY = y + padding

    for (line <- lines) {
      val s = fontRenderer.trimStringToWidth(line, bufferWidth - padding * 2)
      if (s.length > 0) {
        fontRenderer.drawString(s, currentX, currentY, 0xFFFFFF)
        currentY += fontRenderer.FONT_HEIGHT
      }
      if (currentY > bufferHeight + y) {
        return
      }
    }
  }

  override def doesGuiPauseGame = false
}