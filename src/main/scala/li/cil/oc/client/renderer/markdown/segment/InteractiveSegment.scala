package li.cil.oc.client.renderer.markdown.segment

trait InteractiveSegment extends Segment {
  def tooltip: Option[String] = None

  def link: Option[String] = None

  private[markdown] def notifyHover(): Unit = {}

  private[markdown] def checkHovered(mouseX: Int, mouseY: Int, x: Int, y: Int, w: Int, h: Int): Option[InteractiveSegment] = if (mouseX >= x && mouseY >= y && mouseX <= x + w && mouseY <= y + h) Some(this) else None
}
