package li.cil.oc.integration.jei

import java.awt.Rectangle
import java.util

import li.cil.oc.client.gui.Relay
import mezz.jei.api.gui.IAdvancedGuiHandler

import scala.collection.convert.WrapAsJava._

object RelayGuiHandler extends IAdvancedGuiHandler[Relay] {

  override def getGuiContainerClass: Class[Relay] = classOf[Relay]

  override def getGuiExtraAreas(gui: Relay): util.List[Rectangle] = List(
    new Rectangle(gui.windowX + gui.tabPosition.getX, gui.windowY + gui.tabPosition.getY, gui.tabPosition.getWidth, gui.tabPosition.getHeight)
  )

  override def getIngredientUnderMouse(guiContainer: Relay, mouseX: Int, mouseY: Int) = null
}
