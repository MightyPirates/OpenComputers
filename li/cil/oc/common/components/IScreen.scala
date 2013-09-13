package li.cil.oc.common.components

import li.cil.oc.server.components.IComponent

trait IScreen {
  def resolution_=(value: (Int, Int)): Unit
  
  def resolution {} // Required for setter.

  def set(col: Int, row: Int, s: String): Unit

  def fill(col: Int, row: Int, w: Int, h: Int, c: Char): Unit

  def copy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int): Unit
}