package li.cil.oc.server.components

import li.cil.oc.common.components.IScreen
import li.cil.oc.common.tileentity.TileEntityScreen
import li.cil.oc.server.PacketSender

class Screen(val owner: TileEntityScreen) extends IScreen with IComponent {
  id = 2

  def resolution_=(value: (Int, Int)) = {
    val (w, h) = value
    PacketSender.sendScreenResolutionChange(owner, w, h)
  }
  def resolution = throw new NotImplementedError
  def set(col: Int, row: Int, s: String) =
    PacketSender.sendScreenSet(owner, col, row, s)

  def fill(col: Int, row: Int, w: Int, h: Int, c: Char) =
    PacketSender.sendScreenFill(owner, col, row, w, h, c)

  def copy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) =
    PacketSender.sendScreenCopy(owner, col, row, w, h, tx, ty)
}