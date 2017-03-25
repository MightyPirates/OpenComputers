package li.cil.oc.integration.jei

import java.awt.Rectangle
import java.util

import li.cil.oc.client.gui.GuiRelay
import mezz.jei.api.gui.IAdvancedGuiHandler

import scala.collection.convert.WrapAsJava._

object RelayGuiHandler extends IAdvancedGuiHandler[GuiRelay] {

  override def getGuiContainerClass: Class[GuiRelay] = classOf[GuiRelay]

  override def getGuiExtraAreas(gui: GuiRelay): util.List[Rectangle] = List(
    new Rectangle(gui.windowX + gui.tabPosition.getX, gui.windowY + gui.tabPosition.getY, gui.tabPosition.getWidth, gui.tabPosition.getHeight)
  )

  override def getIngredientUnderMouse(guiContainer: GuiRelay, mouseX: Int, mouseY: Int) = null
}
