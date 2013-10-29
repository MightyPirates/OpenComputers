package li.cil.oc.client.gui

import li.cil.oc.Config
import li.cil.oc.client.PacketSender
import li.cil.oc.common.tileentity
import net.minecraft.client.gui.{GuiScreen => MCGuiScreen}
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import scala.collection.mutable

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
class Screen(tileEntity: tileentity.Screen) extends MCGuiScreen {
  val screen = tileEntity.origin
  screen.currentGui = Some(this)

  private var (x, y, innerWidth, innerHeight, scale) = (0, 0, 0, 0, 0.0)

  private val pressedKeys = mutable.Map.empty[Int, Char]

  /** Must be called when the size of the underlying screen changes */
  def changeSize(w: Double, h: Double) = {
    // Re-compute sizes and positions.
    val totalMargin = Screen.margin + Screen.innerMargin
    val bufferWidth = w * MonospaceFontRenderer.fontWidth
    val bufferHeight = h * MonospaceFontRenderer.fontHeight
    val bufferScaleX = (width / (bufferWidth + totalMargin * 2.0)) min 1
    val bufferScaleY = (height / (bufferHeight + totalMargin * 2.0)) min 1
    scale = bufferScaleX min bufferScaleY
    innerWidth = (bufferWidth * scale).toInt
    innerHeight = (bufferHeight * scale).toInt
    x = (width - (innerWidth + totalMargin * 2)) / 2
    y = (height - (innerHeight + totalMargin * 2)) / 2

    // Re-build display lists.
    Screen.compileBackground(innerWidth, innerHeight)
    Screen.compileText(scale, screen.instance.lines)
  }

  /** Must be called whenever the buffer of the underlying screen changes. */
  def updateText() = Screen.compileText(scale, screen.instance.lines)

  override def handleKeyboardInput() {
    super.handleKeyboardInput()

    val code = Keyboard.getEventKey
    if (code != Keyboard.KEY_ESCAPE && code != Keyboard.KEY_F11)
      if (code == Keyboard.KEY_INSERT && MCGuiScreen.isShiftKeyDown) {
        if (Keyboard.getEventKeyState)
          PacketSender.sendClipboard(screen, MCGuiScreen.getClipboardString)
      }
      else if (Keyboard.getEventKeyState) {
        val char = Keyboard.getEventCharacter
        PacketSender.sendKeyDown(screen, char, code)
        pressedKeys += code -> char
      }
      else pressedKeys.remove(code) match {
        case Some(char) => PacketSender.sendKeyUp(screen, char, code)
        case _ => // Wasn't pressed while viewing the screen.
      }
  }

  protected override def mouseClicked(x: Int, y: Int, button: Int) {
    super.mouseClicked(x, y, button)
    if (button == 2)
      PacketSender.sendClipboard(screen, MCGuiScreen.getClipboardString)
  }

  override def initGui() = {
    super.initGui()
    MonospaceFontRenderer.init(mc.renderEngine)
    Screen.init(mc.renderEngine)
    val (w, h) = screen.instance.resolution
    changeSize(w, h)
    Keyboard.enableRepeatEvents(true)
  }

  override def onGuiClosed() = {
    super.onGuiClosed()
    screen.currentGui = None
    for ((code, char) <- pressedKeys) {
      PacketSender.sendKeyUp(screen, char, code)
    }
    Keyboard.enableRepeatEvents(false)
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float): Unit = {
    GL11.glPushMatrix()
    GL11.glTranslatef(x, y, 0)
    GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
    Screen.draw()
    GL11.glPopMatrix()

    super.drawScreen(mouseX, mouseY, dt)
  }

  override def doesGuiPauseGame = false
}

/** We cache OpenGL stuff in a singleton to avoid having to re-allocate. */
object Screen {
  val margin = 7

  val innerMargin = 1

  private val borders = new ResourceLocation(Config.resourceDomain, "textures/gui/borders.png")

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

  private[gui] def compileBackground(bufferWidth: Int, bufferHeight: Int) =
    if (textureManager.isDefined) {
      val innerWidth = innerMargin * 2 + bufferWidth
      val innerHeight = innerMargin * 2 + bufferHeight

      GL11.glNewList(displayLists.get, GL11.GL_COMPILE)

      textureManager.get.bindTexture(borders)

      // Top border (left corner, middle bar, right corner).
      drawBorder(
        0, 0, margin, margin,
        0, 0, 7, 7)
      drawBorder(
        margin, 0, innerWidth, margin,
        7, 0, 8, 7)
      drawBorder(
        margin + innerWidth, 0, margin, margin,
        8, 0, 15, 7)

      // Middle area (left bar, screen background, right bar).
      drawBorder(
        0, margin, margin, innerHeight,
        0, 7, 7, 8)
      drawBorder(
        margin, margin, innerWidth, innerHeight,
        7, 7, 8, 8)
      drawBorder(
        margin + innerWidth, margin, margin, innerHeight,
        8, 7, 15, 8)

      // Bottom border (left corner, middle bar, right corner).
      drawBorder(
        0, margin + innerHeight, margin, margin,
        0, 8, 7, 15)
      drawBorder(
        margin, margin + innerHeight, innerWidth, margin,
        7, 8, 8, 15)
      drawBorder(
        margin + innerWidth, margin + innerHeight, margin, margin,
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
}