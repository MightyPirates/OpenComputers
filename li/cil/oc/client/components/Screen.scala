package li.cil.oc.client.components

import li.cil.oc.common.components.IScreen
import li.cil.oc.common.tileentity.TileEntityScreen
import li.cil.oc.common.util.TextBuffer

class Screen(owner: TileEntityScreen) extends IScreen {
  val buffer = new TextBuffer(40, 24)

  override def toString = buffer.toString
  
  def resolution = buffer.size
  def resolution_=(value: (Int, Int)) = {
    buffer.size = value
    owner.updateGui(buffer.toString)
  }

  def set(col: Int, row: Int, s: String) = {
    buffer.set(col, row, s)
    owner.updateGui(buffer.toString)
  }

  def fill(col: Int, row: Int, w: Int, h: Int, c: Char) = {
    buffer.fill(col, row, w, h, c)
    owner.updateGui(buffer.toString)
  }

  def copy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) = {
    buffer.copy(col, row, w, h, tx, ty)
    owner.updateGui(buffer.toString)
  }
}