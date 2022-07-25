package li.cil.oc.client.gui.widget

import com.mojang.blaze3d.matrix.MatrixStack

@Deprecated
abstract class Widget {
  var owner: WidgetContainer = _

  def x: Int

  def y: Int

  def width: Int

  def height: Int

  def draw(stack: MatrixStack): Unit
}
