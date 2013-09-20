package li.cil.oc.client.gui

import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.client.gui.Gui
import org.lwjgl.opengl.GL11
import net.minecraft.util.ResourceLocation
import net.minecraft.client.renderer.Tessellator

class GuiScreen(val tileEntity: TileEntityScreen) extends net.minecraft.client.gui.GuiScreen {
  tileEntity.gui = Some(this)

  private val borders = new ResourceLocation("opencomputers", "textures/gui/borders.png")

  private val margin = 7
  private val innerMargin = 1

  var (x, y, innerWidth, innerHeight, scale) = (0, 0, 0, 0, 0.0)

  def setSize(w: Double, h: Double) = {
    val totalMargin = (margin + innerMargin) * 2
    val bufferWidth = w * MonospaceFontRenderer.fontWidth
    val bufferHeight = h * MonospaceFontRenderer.fontHeight
    val bufferScaleX = ((width - totalMargin) / bufferWidth) min 1
    val bufferScaleY = ((height - totalMargin) / bufferHeight) min 1
    scale = bufferScaleX min bufferScaleY
    innerWidth = (bufferWidth * scale + 1).ceil.toInt
    innerHeight = (bufferHeight * scale + 1).ceil.toInt
    x = (width - (innerWidth + totalMargin)) / 2
    y = (height - (innerHeight + totalMargin)) / 2
  }

  override def initGui() = {
    super.initGui()
    MonospaceFontRenderer.init(mc.renderEngine)
    val (w, h) = tileEntity.component.resolution
    setSize(w, h)
  }

  override def onGuiClosed = {
    super.onGuiClosed()
    tileEntity.gui = None
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float): Unit = {
    GL11.glPushMatrix()
    GL11.glTranslatef(x, y, 0)
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
    setTexture(borders)

    drawRectangle(
      0, 0, 7, 7,
      0, 0, 7, 7)
    drawRectangle(
      margin, 0, innerWidth, 7,
      7, 0, 8, 7)
    drawRectangle(
      margin + innerWidth, 0, 7, 7,
      8, 0, 15, 7)

    drawRectangle(
      0, margin, 7, innerHeight,
      0, 7, 7, 8)
    drawRectangle(
      margin, margin, innerWidth, innerHeight,
      7, 7, 8, 8)
    drawRectangle(
      margin + innerWidth, margin, 7, innerHeight,
      8, 7, 15, 8)

    drawRectangle(
      0, margin + innerHeight, 7, 7,
      0, 8, 7, 15)
    drawRectangle(
      margin, margin + innerHeight, innerWidth, 7,
      7, 8, 8, 15)
    drawRectangle(
      margin + innerWidth, margin + innerHeight, 7, 7,
      8, 8, 15, 15)

    GL11.glTranslatef(margin + innerMargin, margin + innerMargin, 0)
    GL11.glScaled(scale, scale, 1)

    tileEntity.component.lines.zipWithIndex.foreach {
      case (line, i) => MonospaceFontRenderer.drawString(line, 0, i * MonospaceFontRenderer.fontHeight)
    }
    GL11.glPopMatrix()

    super.drawScreen(mouseX, mouseY, dt);
  }

  override def doesGuiPauseGame = false

  private def setTexture(value: ResourceLocation) =
    mc.renderEngine.func_110577_a(value)

  private def drawRectangle(x: Double, y: Double, w: Double, h: Double, u1: Int, v1: Int, u2: Int, v2: Int) = {
    val t = Tessellator.instance
    val (u1d, u2d, v1d, v2d) = (u1 / 16.0, u2 / 16.0, v1 / 16.0, v2 / 16.0)
    t.startDrawingQuads()
    t.addVertexWithUV(x, y + h, 0, u1d, v2d)
    t.addVertexWithUV(x + w, y + h, 0, u2d, v2d)
    t.addVertexWithUV(x + w, y, 0, u2d, v1d)
    t.addVertexWithUV(x, y, 0, u1d, v1d)
    t.draw()
  }
}