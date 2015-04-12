package li.cil.oc.client.renderer.markdown.segment

/**
 * Segments that can react to mouse presence and input.
 *
 * The currently hovered interactive segment is picked in the render process
 * and returned there. Calling code can then decide whether to render the
 * segment's tooltip, for example. It should also notice the currently hovered
 * segment when a left-click occurs.
 */
trait InteractiveSegment extends Segment {
  /**
   * The tooltip that should be displayed when this segment is being hovered.
   */
  def tooltip: Option[String] = None

  /**
   * Should be called by whatever is rendering the document when a left mouse
   * click occurs.
   *
   * The mouse coordinates are expected to be in the same frame of reference as
   * the document.
   *
   * @param mouseX the X coordinate of the mouse cursor.
   * @param mouseY the Y coordinate of the mouse cursor.
   * @return whether the click was processed (true) or ignored (false).
   */
  def onMouseClick(mouseX: Int, mouseY: Int): Boolean = false

  // Called during the render call on the currently hovered interactive segment.
  // Useful to track hover state, e.g. for link highlighting.
  private[markdown] def notifyHover(): Unit = {}

  // Collision check, test if coordinate is inside this interactive segment.
  private[markdown] def checkHovered(mouseX: Int, mouseY: Int, x: Int, y: Int, w: Int, h: Int): Option[InteractiveSegment] =
    if (mouseX >= x && mouseY >= y && mouseX <= x + w && mouseY <= y + h) Some(this) else None
}
