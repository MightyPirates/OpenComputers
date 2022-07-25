package li.cil.oc.client.gui

import java.util

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.gui.widget.WidgetContainer
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.screen.inventory.ContainerScreen
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.container.Container
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.ITextProperties
import net.minecraft.util.text.LanguageMap
import net.minecraft.util.text.StringTextComponent

import scala.collection.convert.WrapAsScala._

// Workaround because certain other mods *cough*TMI*cough* do base class
// transformations that break things! Such fun. Many annoyed. And yes, this
// is a common issue, have a look at EnderIO and Enchanting Plus. They have
// to work around this, too.
abstract class CustomGuiContainer[C <: Container](val inventoryContainer: C, inv: PlayerInventory, title: ITextComponent)
  extends ContainerScreen(inventoryContainer, inv, title) with WidgetContainer {

  override def windowX = leftPos

  override def windowY = topPos

  override def windowZ = getBlitOffset

  override def isPauseScreen = false

  protected def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  // Pretty much Scalaified copy-pasta from base-class.
  override def renderWrappedToolTip(stack: MatrixStack, text: util.List[_ <: ITextProperties], x: Int, y: Int, font: FontRenderer): Unit = {
    copiedDrawHoveringText0(stack, text, x, y, font)
  }

  protected def isPointInRegion(rectX: Int, rectY: Int, rectWidth: Int, rectHeight: Int, pointX: Int, pointY: Int): Boolean =
    pointX >= rectX - 1 && pointX < rectX + rectWidth + 1 && pointY >= rectY - 1 && pointY < rectY + rectHeight + 1

  protected def copiedDrawHoveringText(stack: MatrixStack, lines: util.List[String], x: Int, y: Int, font: FontRenderer): Unit = {
    val text = new util.ArrayList[StringTextComponent]()
    for (line <- lines) {
      text.add(new StringTextComponent(line))
    }
    copiedDrawHoveringText0(stack, text, x, y, font)
  }

  protected def copiedDrawHoveringText0(stack: MatrixStack, text: util.List[_ <: ITextProperties], x: Int, y: Int, font: FontRenderer): Unit = {
    if (!text.isEmpty) {
      RenderSystem.disableRescaleNormal()
      RenderHelper.turnOff()
      RenderSystem.disableLighting()
      RenderSystem.disableDepthTest()

      val textWidth = text.map(line => font.width(line)).max

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

      setBlitOffset(300)
      itemRenderer.blitOffset = 300f
      val bg = 0xF0100010
      fillGradient(stack, posX - 3, posY - 4, posX + textWidth + 3, posY - 3, bg, bg)
      fillGradient(stack, posX - 3, posY + textHeight + 3, posX + textWidth + 3, posY + textHeight + 4, bg, bg)
      fillGradient(stack, posX - 3, posY - 3, posX + textWidth + 3, posY + textHeight + 3, bg, bg)
      fillGradient(stack, posX - 4, posY - 3, posX - 3, posY + textHeight + 3, bg, bg)
      fillGradient(stack, posX + textWidth + 3, posY - 3, posX + textWidth + 4, posY + textHeight + 3, bg, bg)
      val color1 = 0x505000FF
      val color2 = (color1 & 0x00FEFEFE) >> 1 | (color1 & 0xFF000000)
      fillGradient(stack, posX - 3, posY - 3 + 1, posX - 3 + 1, posY + textHeight + 3 - 1, color1, color2)
      fillGradient(stack, posX + textWidth + 2, posY - 3 + 1, posX + textWidth + 3, posY + textHeight + 3 - 1, color1, color2)
      fillGradient(stack, posX - 3, posY - 3, posX + textWidth + 3, posY - 3 + 1, color1, color1)
      fillGradient(stack, posX - 3, posY + textHeight + 2, posX + textWidth + 3, posY + textHeight + 3, color2, color2)

      stack.pushPose()
      stack.translate(0, 0, 400)
      val renderType = IRenderTypeBuffer.immediate(Tessellator.getInstance.getBuilder())
      for ((line, index) <- text.zipWithIndex) {
        font.drawInBatch(LanguageMap.getInstance.getVisualOrder(line), posX, posY, -1, true, stack.last.pose, renderType, false, 0, 15728880)
        if (index == 0) {
          posY += 2
        }
        posY += 10
      }
      stack.popPose()
      setBlitOffset(0)
      itemRenderer.blitOffset = 0f

      RenderSystem.enableLighting()
      RenderSystem.enableDepthTest()
      RenderHelper.turnBackOn()
      RenderSystem.enableRescaleNormal()
    }
  }

  override def fillGradient(stack: MatrixStack, left: Int, top: Int, right: Int, bottom: Int, startColor: Int, endColor: Int): Unit = {
    super.fillGradient(stack, left, top, right, bottom, startColor, endColor)
    RenderState.makeItBlend()
  }

  override def render(stack: MatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    this.renderBackground(stack)
    super.render(stack, mouseX, mouseY, partialTicks)
    this.renderTooltip(stack, mouseX, mouseY)
  }
}
