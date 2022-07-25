package li.cil.oc.client.gui

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Localization
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.markdown.Document
import li.cil.oc.client.renderer.markdown.segment.InteractiveSegment
import li.cil.oc.client.renderer.markdown.segment.Segment
import li.cil.oc.client.{Manual => ManualAPI}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screen
import net.minecraft.client.gui.widget.button.Button
import net.minecraft.client.util.InputMappings
import net.minecraft.util.text.ITextProperties
import net.minecraft.util.text.StringTextComponent
import org.lwjgl.glfw.GLFW

import scala.collection.JavaConverters.{asJavaIterable, seqAsJavaList}
import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._

class Manual extends screen.Screen(StringTextComponent.EMPTY) with traits.Window {
  final val documentMaxWidth = 230
  final val documentMaxHeight = 176
  final val scrollPosX = 244
  final val scrollPosY = 6
  final val scrollWidth = 6
  final val scrollHeight = 180
  final val tabPosX = -23
  final val tabPosY = 7
  final val tabWidth = 23
  final val tabHeight = 26
  final val maxTabsPerSide = 7

  override val windowWidth = 256
  override val windowHeight = 192

  override def backgroundImage = Textures.GUI.Manual

  var document: Segment = null
  var documentHeight = 0
  var currentSegment = None: Option[InteractiveSegment]
  protected var scrollButton: ImageButton = _

  private def canScroll = maxOffset > 0

  def offset = ManualAPI.history.top.offset

  def maxOffset = documentHeight - documentMaxHeight

  def resolveLink(path: String, current: String): String =
    if (path.startsWith("/")) path
    else {
      val splitAt = current.lastIndexOf('/')
      if (splitAt >= 0) current.splitAt(splitAt)._1 + "/" + path
      else path
    }

  def refreshPage(): Unit = {
    val content = Option(api.Manual.contentFor(ManualAPI.history.top.path)).
      getOrElse(asJavaIterable(Iterable("Document not found: " + ManualAPI.history.top.path)))
    document = Document.parse(content)
    documentHeight = Document.height(document, documentMaxWidth, font)
    scrollTo(offset)
  }

  def pushPage(path: String): Unit = {
    if (path != ManualAPI.history.top.path) {
      ManualAPI.history.push(new ManualAPI.History(path))
      refreshPage()
    }
  }

  def popPage(): Unit = {
    if (ManualAPI.history.size > 1) {
      ManualAPI.history.pop()
      refreshPage()
    }
    else {
      minecraft.player.closeContainer()
    }
  }

  override protected def init(): Unit = {
    super.init()

    for ((tab, i) <- ManualAPI.tabs.zipWithIndex if i < maxTabsPerSide) {
      val x = leftPos + tabPosX
      val y = topPos + tabPosY + i * (tabHeight - 1)
      addButton(new ImageButton(x, y, tabWidth, tabHeight, new Button.IPressable {
        override def onPress(b: Button) = api.Manual.navigate(tab.path)
      }, Textures.GUI.ManualTab))
    }

    scrollButton = new ImageButton(leftPos + scrollPosX, topPos + scrollPosY, 6, 13, new Button.IPressable {
      override def onPress(b: Button) = Unit
    }, Textures.GUI.ButtonScroll)
    addButton(scrollButton)

    refreshPage()
  }

  override def render(stack: MatrixStack, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    super.render(stack, mouseX, mouseY, dt)

    scrollButton.active = canScroll
    scrollButton.hoverOverride = isDragging

    for ((tab, i) <- ManualAPI.tabs.zipWithIndex if i < maxTabsPerSide) {
      val button = buttons.get(i).asInstanceOf[ImageButton]
      stack.pushPose()
      stack.translate(button.x + 5, button.y + 5, getBlitOffset)
      tab.renderer.render()
      stack.popPose()
    }

    currentSegment = Document.render(stack, document, leftPos + 8, topPos + 8, documentMaxWidth, documentMaxHeight, offset, font, mouseX, mouseY)
    def localizeAndWrap(text: String): java.util.List[_ <: ITextProperties] = {
      val lines = Localization.localizeImmediately(text).lines.map(new StringTextComponent(_))
      seqAsJavaList(lines.toSeq)
    }

    if (!isDragging) currentSegment match {
      case Some(segment) =>
        segment.tooltip match {
          case Some(text) if text.nonEmpty => renderWrappedToolTip(stack, localizeAndWrap(text), mouseX, mouseY, font)
          case _ =>
        }
      case _ =>
    }

    if (!isDragging) for ((tab, i) <- ManualAPI.tabs.zipWithIndex if i < maxTabsPerSide) {
      val button = buttons.get(i).asInstanceOf[ImageButton]
      if (mouseX > button.x && mouseX < button.x + tabWidth && mouseY > button.y && mouseY < button.y + tabHeight) tab.tooltip.foreach(text => {
        renderWrappedToolTip(stack, localizeAndWrap(text), mouseX, mouseY, font)
      })
    }

    if (canScroll && (isCoordinateOverScrollBar(mouseX - leftPos, mouseY - topPos) || isDragging)) {
      val lines = seqAsJavaList(Seq(new StringTextComponent(s"${100 * offset / maxOffset}%")))
      renderWrappedToolTip(stack, lines, leftPos + scrollPosX + scrollWidth, scrollButton.y + scrollButton.getHeight + 1, font)
    }
  }

  override def keyPressed(keyCode: Int, scanCode: Int, mods: Int): Boolean = {
    val input = InputMappings.getKey(keyCode, scanCode)
    if (minecraft.options.keyJump.isActiveAndMatches(input)) {
      popPage()
      return true
    }
    if (minecraft.options.keyInventory.isActiveAndMatches(input)) {
      onClose()
      return true
    }
    super.keyPressed(keyCode, scanCode, mods)
  }

  override def mouseScrolled(mouseX: Double, mouseY: Double, scroll: Double): Boolean = {
    if (scroll < 0) scrollDown()
    else scrollUp()
    true
  }

  override def mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    if (canScroll && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isCoordinateOverScrollBar(mouseX.asInstanceOf[Int] - leftPos, mouseY.asInstanceOf[Int] - topPos)) {
      setDragging(true)
      scrollMouse(mouseY)
      return true
    }
    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
      currentSegment.foreach(_.onMouseClick(mouseX.asInstanceOf[Int], mouseY.asInstanceOf[Int]))
      return true
    }
    if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
      popPage()
      return true
    }
    super.mouseClicked(mouseX, mouseY, button)
  }

  override def mouseMoved(mouseX: Double, mouseY: Double): Unit = {
    if (isDragging) scrollMouse(mouseY)
    super.mouseMoved(mouseX, mouseY)
  }

  override def mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = {
    if (isDragging) {
      scrollMouse(mouseY)
      return true
    }
    super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
  }

  override def mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
      setDragging(false)
      return true
    }
    super.mouseReleased(mouseX, mouseY, button)
  }

  private def scrollMouse(mouseY: Double) {
    scrollTo(math.round((mouseY - topPos - scrollPosY - 6.5) * maxOffset / (scrollHeight - 13.0)).toInt)
  }

  private def scrollUp() = scrollTo(offset - Document.lineHeight(font) * 3)

  private def scrollDown() = scrollTo(offset + Document.lineHeight(font) * 3)

  private def scrollTo(row: Int): Unit = {
    ManualAPI.history.top.offset = math.max(0, math.min(maxOffset, row))
    val yMin = topPos + scrollPosY
    if (maxOffset > 0) {
      scrollButton.y = yMin + (scrollHeight - 13) * offset / maxOffset
    }
    else {
      scrollButton.y = yMin
    }
  }

  private def isCoordinateOverScrollBar(x: Int, y: Int) =
    x > scrollPosX && x < scrollPosX + scrollWidth &&
      y >= scrollPosY && y < scrollPosY + scrollHeight
}
