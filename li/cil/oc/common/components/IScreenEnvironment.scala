package li.cil.oc.common.components

import li.cil.oc.api.{INetworkMessage, INetworkNode}

/**
 * Environment for screen components.
 *
 * The environment of a screen is responsible for synchronizing the component
 * between server and client. These callbacks are only called on the server
 * side to trigger changes being sent to clients and saving the current state.
 */
trait IScreenEnvironment extends INetworkNode {
  val screen = new Screen(this)

  override def name = "screen"

  override def receive(message: INetworkMessage): Option[Array[Any]] = message.data match {
    case Array(w: Int, h: Int) if message.name == "screen.resolution=" =>
      Some(Array((screen.resolution = (w, h)): Any))
    case Array() if message.name == "screen.resolution" => {
      val (w, h) = screen.resolution
      Some(Array(w: Any, h: Any))
    }
    case Array() if message.name == "screen.resolutions" =>
      Some(Array(screen.supportedResolutions: _*))
    case Array(x: Int, y: Int, value: String) if message.name == "screen.set" =>
      screen.set(x, y, value); None
    case Array(x: Int, y: Int, w: Int, h: Int, value: Char) if message.name == "screen.fill" =>
      screen.fill(x, y, w, h, value); None
    case Array(x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int) if message.name == "screen.copy" =>
      screen.copy(x, y, w, h, tx, ty); None
    case _ => None
  }

  def onScreenResolutionChange(w: Int, h: Int)

  def onScreenSet(col: Int, row: Int, s: String)

  def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char)

  def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int)
}