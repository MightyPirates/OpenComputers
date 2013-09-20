package li.cil.oc.client.gui

import li.cil.oc.common.tileentity.TileEntityScreen
import net.minecraft.client.gui.Gui
import org.lwjgl.opengl.GL11
import net.minecraft.util.ResourceLocation
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.texture.TextureManager

/**
 * This GUI shows the buffer of a single screen.
 *
 * The window is sized to fit the contents of the buffer. If the buffer is
 * smaller than the overall available space, the window is centered. if it is
 * larger, the content is scaled down to fit the available space.
 *
 * Text and background are cached in display lists, so updateText() must be
 * called whenever the text actually changes, otherwise there will be no change
 * in the text displayed in the GUI.
 */
class GuiScreen(val tileEntity: TileEntityScreen) extends net.minecraft.client.gui.GuiScreen {
  tileEntity.gui = Some(this)

  var (x, y, innerWidth, innerHeight, scale) = (0, 0, 0, 0, 0.0)

  /** Must be called when the size of the underlying screen changes */
  def setSize(w: Double, h: Double) = {
    // Re-compute sizes and positions.
    val totalMargin = (GuiScreen.margin + GuiScreen.innerMargin) * 2
    val bufferWidth = w * MonospaceFontRenderer.fontWidth
    val bufferHeight = h * MonospaceFontRenderer.fontHeight
    val bufferScaleX = ((width - totalMargin) / bufferWidth) min 1
    val bufferScaleY = ((height - totalMargin) / bufferHeight) min 1
    scale = bufferScaleX min bufferScaleY
    innerWidth = (bufferWidth * scale + 1).ceil.toInt
    innerHeight = (bufferHeight * scale + 1).ceil.toInt
    x = (width - (innerWidth + totalMargin)) / 2
    y = (height - (innerHeight + totalMargin)) / 2

    // Re-build display lists.
    GuiScreen.compileBackground(innerWidth, innerHeight)
    GuiScreen.compileText(scale, tileEntity.component.lines)
  }

  /** Must be called whenever the buffer of the underlying screen changes. */
  def updateText() = GuiScreen.compileText(scale, tileEntity.component.lines)

  override def initGui() = {
    super.initGui()
    MonospaceFontRenderer.init(mc.renderEngine)
    GuiScreen.init(mc.renderEngine)
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
    GuiScreen.draw()
    GL11.glPopMatrix()

    super.drawScreen(mouseX, mouseY, dt);
  }

  override def doesGuiPauseGame = false
}

/** We cache OpenGL stuff in a singleton to avoid having to re-allocate. */
object GuiScreen {
  val margin = 7

  val innerMargin = 1

  private val borders = new ResourceLocation("opencomputers", "textures/gui/borders.png")

  private var textureManager: Option[TextureManager] = None

  private var displayLists: Option[Int] = None

  private var buffer: Option[java.nio.IntBuffer] = None

  def init(tm: TextureManager) = if (!textureManager.isDefined) {
    textureManager = Some(tm)
    displayLists = Some(GLAllocation.generateDisplayLists(2))
    buffer = Some(GLAllocation.createDirectIntBuffer(2))
    buffer.get.put(displayLists.get)
    buffer.get.put(displayLists.get + 1)
  }

  def draw() = if (textureManager.isDefined) {
    buffer.get.rewind()
    GL11.glCallLists(buffer.get)
  }

  private[gui] def compileBackground(innerWidth: Int, innerHeight: Int) =
    if (textureManager.isDefined) {
      GL11.glNewList(displayLists.get, GL11.GL_COMPILE)

      setTexture(borders)

      // Top border (left corner, middle bar, right corner).
      drawBorder(
        0, 0, 7, 7,
        0, 0, 7, 7)
      drawBorder(
        margin, 0, innerWidth, 7,
        7, 0, 8, 7)
      drawBorder(
        margin + innerWidth, 0, 7, 7,
        8, 0, 15, 7)

      // Middle area (left bar, screen background, right bar).
      drawBorder(
        0, margin, 7, innerHeight,
        0, 7, 7, 8)
      drawBorder(
        margin, margin, innerWidth, innerHeight,
        7, 7, 8, 8)
      drawBorder(
        margin + innerWidth, margin, 7, innerHeight,
        8, 7, 15, 8)

      // Bottom border (left corner, middle bar, right corner).
      drawBorder(
        0, margin + innerHeight, 7, 7,
        0, 8, 7, 15)
      drawBorder(
        margin, margin + innerHeight, innerWidth, 7,
        7, 8, 8, 15)
      drawBorder(
        margin + innerWidth, margin + innerHeight, 7, 7,
        8, 8, 15, 15)

      GL11.glEndList()
    }

  private[gui] def compileText(scale: Double, lines: Array[Array[Char]]) =
    if (textureManager.isDefined) {
      GL11.glNewList(displayLists.get + 1, GL11.GL_COMPILE)

      GL11.glTranslatef(margin + innerMargin, margin + innerMargin, 0)
      GL11.glScaled(scale, scale, 1)
      lines.zipWithIndex.foreach {
        case (line, i) => MonospaceFontRenderer.drawString(line, 0, i * MonospaceFontRenderer.fontHeight)
      }

      GL11.glEndList()
    }

  private def drawBorder(x: Double, y: Double, w: Double, h: Double, u1: Int, v1: Int, u2: Int, v2: Int) = {
    val (u1d, u2d, v1d, v2d) = (u1 / 16.0, u2 / 16.0, v1 / 16.0, v2 / 16.0)
    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(x, y + h, 0, u1d, v2d)
    t.addVertexWithUV(x + w, y + h, 0, u2d, v2d)
    t.addVertexWithUV(x + w, y, 0, u2d, v1d)
    t.addVertexWithUV(x, y, 0, u1d, v1d)
    t.draw()
  }

  private def setTexture(value: ResourceLocation) =
    textureManager.get.func_110577_a(value)
}