package li.cil.oc.client.gui.traits

import java.util

import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.ResourceLocation

trait Window extends GuiScreen {
  var guiLeft = 0
  var guiTop = 0
  var xSize = 0
  var ySize = 0

  val windowWidth = 176
  val windowHeight = 166

  def backgroundImage: ResourceLocation

  protected def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  override def doesGuiPauseGame = false

  override def initGui(): Unit = {
    super.initGui()

    val screenSize = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight)
    val guiSize = new ScaledResolution(mc, windowWidth, windowHeight)
    val (midX, midY) = (screenSize.getScaledWidth / 2, screenSize.getScaledHeight / 2)
    guiLeft = midX - guiSize.getScaledWidth / 2
    guiTop = midY - guiSize.getScaledHeight / 2
    xSize = guiSize.getScaledWidth
    ySize = guiSize.getScaledHeight
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float): Unit = {
    mc.renderEngine.bindTexture(backgroundImage)
    Gui.func_146110_a(guiLeft, guiTop, 0, 0, xSize, ySize, windowWidth, windowHeight)

    super.drawScreen(mouseX, mouseY, dt)
  }

}
