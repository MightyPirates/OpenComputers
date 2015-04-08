package li.cil.oc.client.gui

import java.util

import li.cil.oc.client.Textures
import li.cil.oc.util.PseudoMarkdown
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import org.lwjgl.input.Mouse

class Manual extends GuiScreen {
  var guiLeft = 0
  var guiTop = 0
  var xSize = 0
  var ySize = 0
  var offset = 0
  var documentHeight = 0
  final val documentMaxWidth = 230
  final val documentMaxHeight = 176
  final val scrollPosX = 244
  final val scrollPosY = 6
  final val scrollHeight = 180

  val document = PseudoMarkdown.parse( """# Headline with more lines  [with link](huehue) and *some* more
                                         |
                                         |The Adapter block is the core of most of OpenComputers' mod integration.
                                         |
                                         |*This* is *italic* text, ~~strikethrough~~ maybe a-ter **some** text **in bold**. Is _this underlined_? Oh, no, _it's also italic!_ Well, this \*isn't bold*.
                                         |
                                         |## Smaller headline [also with link but this one longer](huehue)
                                         |
                                         |This is *italic
                                         |over two* lines. But *this ... no *this is* **_bold italic_** *text*.
                                         |
                                         |### even smaller
                                         |
                                         |*not italic *because ** why would it be*eh
                                         |
                                         |isn't*.
                                         |
                                         |   # not a header
                                         |
                                         |![](https://avatars1.githubusercontent.com/u/514903)
                                         |
                                         |And finally, [this is a link!](https://avatars1.githubusercontent.com/u/514903).""".stripMargin)

  def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  protected var scrollButton: ImageButton = _

  override def doesGuiPauseGame = false

  override def initGui(): Unit = {
    super.initGui()

    val mc = Minecraft.getMinecraft
    val screenSize = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight)
    val guiSize = new ScaledResolution(mc, 256, 192)
    val (midX, midY) = (screenSize.getScaledWidth / 2, screenSize.getScaledHeight / 2)
    guiLeft = midX - guiSize.getScaledWidth / 2
    guiTop = midY - guiSize.getScaledHeight / 2
    xSize = guiSize.getScaledWidth
    ySize = guiSize.getScaledHeight
    offset = 0
    documentHeight = PseudoMarkdown.height(document, documentMaxWidth, fontRendererObj)

    scrollButton = new ImageButton(1, guiLeft + scrollPosX, guiTop + scrollPosY, 6, 13, Textures.guiButtonScroll)
    add(buttonList, scrollButton)
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float): Unit = {
    mc.renderEngine.bindTexture(Textures.guiManual)
    Gui.func_146110_a(guiLeft, guiTop, 0, 0, xSize, ySize, 256, 192)

    super.drawScreen(mouseX, mouseY, dt)

    PseudoMarkdown.render(document, guiLeft + 8, guiTop + 8, documentMaxWidth, documentMaxHeight, offset, fontRendererObj, mouseX, mouseY)
  }

  override def handleMouseInput(): Unit = {
    super.handleMouseInput()
    if (Mouse.hasWheel && Mouse.getEventDWheel != 0) {
      if (math.signum(Mouse.getEventDWheel) < 0) scrollDown()
      else scrollUp()
    }
  }

  private def scrollUp() = scrollTo(offset - PseudoMarkdown.lineHeight(fontRendererObj) * 3)

  private def scrollDown() = scrollTo(offset + PseudoMarkdown.lineHeight(fontRendererObj) * 3)

  private def scrollTo(row: Int): Unit = {
    val maxOffset = documentHeight - documentMaxHeight
    offset = math.max(0, math.min(maxOffset, row))
    val yMin = guiTop + scrollPosY
    if (maxOffset > 0) {
      scrollButton.yPosition = yMin + (scrollHeight - 13) * offset / maxOffset
    }
    else {
      scrollButton.yPosition = yMin
    }
  }
}
