package li.cil.oc.client.renderer.font

import li.cil.oc.util.TextBuffer

trait TextBufferRenderData {
  def dirty: Boolean

  def dirty_=(value: Boolean): Unit

  def data: TextBuffer

  def viewport: (Int, Int)
}
