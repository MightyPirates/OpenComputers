package li.cil.oc.common.component

import li.cil.oc.api.network.{Node, Visibility, Message}
import net.minecraft.nbt.NBTTagCompound

/**
 * Environment for screen components.
 *
 * The environment of a screen is responsible for synchronizing the component
 * between server and client. These callbacks are only called on the server
 * side to trigger changes being sent to clients and saving the current state.
 */
trait ScreenEnvironment extends Node {
  val screen = new Screen(this)

  override val name = "screen"

  override val visibility = Visibility.Network

  override def receive(message: Message): Option[Array[Any]] = super.receive(message).orElse {
    message.data match {
      case Array(w: Int, h: Int) if message.name == "screen.resolution=" =>
        result(screen.resolution = (w, h))
      case Array() if message.name == "screen.resolution" => {
        val (w, h) = screen.resolution
        result(w, h)
      }
      case Array() if message.name == "screen.resolutions" =>
        result(screen.supportedResolutions: _*)
      case Array(x: Int, y: Int, value: String) if message.name == "screen.set" =>
        screen.set(x, y, value); None
      case Array(x: Int, y: Int, w: Int, h: Int, value: Char) if message.name == "screen.fill" =>
        screen.fill(x, y, w, h, value); None
      case Array(x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int) if message.name == "screen.copy" =>
        screen.copy(x, y, w, h, tx, ty); None
      case _ => None
    }
  }

  override def load(nbt: NBTTagCompound) = {
    super.load(nbt)
    screen.load(nbt.getCompoundTag("screen"))
  }

  override def save(nbt: NBTTagCompound) = {
    super.save(nbt)

    val screenNbt = new NBTTagCompound
    screen.save(screenNbt)
    nbt.setCompoundTag("screen", screenNbt)

  }

  def onScreenResolutionChange(w: Int, h: Int) =
    network.foreach(_.sendToVisible(this, "computer.signal", "screen_resized", w, h))

  def onScreenSet(col: Int, row: Int, s: String) {}

  def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char) {}

  def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {}
}