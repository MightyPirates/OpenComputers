package li.cil.oc.integration.jei

import java.util

import li.cil.oc.client.gui.Relay
import mezz.jei.api.gui.handlers.IGuiContainerHandler
import net.minecraft.client.renderer.Rectangle2d

import scala.collection.convert.ImplicitConversionsToJava._

object RelayGuiHandler extends IGuiContainerHandler[Relay] {

  override def getGuiExtraAreas(gui: Relay): util.List[Rectangle2d] = List(
    new Rectangle2d(gui.windowX + gui.tabPosition.getX, gui.windowY + gui.tabPosition.getY, gui.tabPosition.getWidth, gui.tabPosition.getHeight)
  )

  override def getIngredientUnderMouse(guiContainer: Relay, mouseX: Double, mouseY: Double) = null
}
