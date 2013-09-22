package li.cil.oc.common.components

/**
 * Environment for screen components.
 *
 * The environment of a screen is responsible for synchronizing the component
 * between server and client. These callbacks are only called on the server
 * side to trigger changes being sent to clients and saving the current state.
 */
trait IScreenEnvironment {
  def onScreenResolutionChange(w: Int, h: Int)

  def onScreenSet(col: Int, row: Int, s: String)

  def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char)

  def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int)
}