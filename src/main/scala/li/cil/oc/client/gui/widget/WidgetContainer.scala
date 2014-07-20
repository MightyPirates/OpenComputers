package li.cil.oc.client.gui.widget

import scala.collection.mutable

trait WidgetContainer {
  protected val widgets = mutable.ArrayBuffer.empty[Widget]

  def addWidget[T <: Widget](widget: T) = {
    widgets += widget
    widget.owner = this
    widget
  }

  def windowX = 0

  def windowY = 0

  def windowZ = 0f

  def drawWidgets() {
    widgets.foreach(_.draw())
  }
}
