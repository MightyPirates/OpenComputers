package li.cil.oc.client.gui

import java.util

import li.cil.oc.client.gui.widget.WidgetContainer
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.inventory.Container

import scala.collection.convert.WrapAsScala._

// Workaround because certain other mods *cough*TMI*cough* do base class
// transformations that break things! Such fun. Many annoyed. And yes, this
// is a common issue, have a look at EnderIO and Enchanting Plus. They have
// to work around this, too.
abstract class CustomGuiContainer[C <: Container](val inventoryContainer: C) extends GuiContainer(inventoryContainer) with WidgetContainer {
  override def windowX = guiLeft

  override def windowY = guiTop

  override def windowZ = zLevel

  override def doesGuiPauseGame = false

  protected def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  // Pretty much Scalaified copy-pasta from base-class.
  override def drawHoveringText(text: util.List[String], x: Int, y: Int, font: FontRenderer): Unit = {
    copiedDrawHoveringText(text, x, y, font)
  }

  protected def copiedDrawHoveringText(text: util.List[String], x: Int, y: Int, font: FontRenderer): Unit = {
    if (!text.isEmpty) {
      GlStateManager.disableRescaleNormal()
      RenderHelper.disableStandardItemLighting()
      GlStateManager.disableLighting()
      GlStateManager.disableDepth()

      val textWidth = text.map(line => font.getStringWidth(line)).max

      var posX = x + 12
      var posY = y - 12
      var textHeight = 8
      if (text.size > 1) {
        textHeight += 2 + (text.size - 1) * 10
      }
      if (posX + textWidth > width) {
        posX -= 28 + textWidth
      }
      if (posY + textHeight + 6 > height) {
        posY = height - textHeight - 6
      }

      zLevel = 300f
      itemRender.zLevel = 300f
      val bg = 0xF0100010
      drawGradientRect(posX - 3, posY - 4, posX + textWidth + 3, posY - 3, bg, bg)
      drawGradientRect(posX - 3, posY + textHeight + 3, posX + textWidth + 3, posY + textHeight + 4, bg, bg)
      drawGradientRect(posX - 3, posY - 3, posX + textWidth + 3, posY + textHeight + 3, bg, bg)
      drawGradientRect(posX - 4, posY - 3, posX - 3, posY + textHeight + 3, bg, bg)
      drawGradientRect(posX + textWidth + 3, posY - 3, posX + textWidth + 4, posY + textHeight + 3, bg, bg)
      val color1 = 0x505000FF
      val color2 = (color1 & 0x00FEFEFE) >> 1 | (color1 & 0xFF000000)
      drawGradientRect(posX - 3, posY - 3 + 1, posX - 3 + 1, posY + textHeight + 3 - 1, color1, color2)
      drawGradientRect(posX + textWidth + 2, posY - 3 + 1, posX + textWidth + 3, posY + textHeight + 3 - 1, color1, color2)
      drawGradientRect(posX - 3, posY - 3, posX + textWidth + 3, posY - 3 + 1, color1, color1)
      drawGradientRect(posX - 3, posY + textHeight + 2, posX + textWidth + 3, posY + textHeight + 3, color2, color2)

      for ((line, index) <- text.zipWithIndex) {
        font.drawStringWithShadow(line.asInstanceOf[String], posX, posY, -1)
        if (index == 0) {
          posY += 2
        }
        posY += 10
      }
      zLevel = 0f
      itemRender.zLevel = 0f

      GlStateManager.enableLighting()
      GlStateManager.enableDepth()
      RenderHelper.enableStandardItemLighting()
      GlStateManager.enableRescaleNormal()
    }
  }

  override def drawGradientRect(left: Int, top: Int, right: Int, bottom: Int, startColor: Int, endColor: Int): Unit = {
    super.drawGradientRect(left, top, right, bottom, startColor, endColor)
    RenderState.makeItBlend()
  }

  override def drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    this.drawDefaultBackground()
    super.drawScreen(mouseX, mouseY, partialTicks)
    this.renderHoveredToolTip(mouseX, mouseY)
  }
}
