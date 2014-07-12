package li.cil.oc.client.renderer.font

trait DynamicCharRenderer {
  def charWidth: Double

  def charHeight: Double

  def drawChar(charCode: Int)
}
