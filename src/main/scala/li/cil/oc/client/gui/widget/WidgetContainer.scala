package li.cil.oc.client.gui.widget

import net.minecraft.client.gui.inventory.GuiContainer

import scala.collection.mutable

trait WidgetContainer { self: GuiContainer =>
  protected val widgets = mutable.ArrayBuffer.empty[Widget]

  def addWidget[T <: Widget](widget: T) = {
    widgets += widget
    widget.owner = this
    widget
  }

  def windowX = guiLeft

  def windowY = guiTop

  def windowZ = zLevel

  def drawWidgets() {
    widgets.foreach(_.draw())
  }
}
