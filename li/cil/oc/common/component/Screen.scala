package li.cil.oc.common.component

import li.cil.oc.api.network.{Message, Visibility, Node}
import li.cil.oc.util.TextBuffer
import net.minecraft.nbt.NBTTagCompound

class Screen(val owner: Screen.Environment) {
  val supportedResolutions = List((40, 24), (80, 24))

  private val buffer = new TextBuffer(80, 24)

  // ----------------------------------------------------------------------- //

  def text = buffer.toString

  def lines = buffer.buffer

  def resolution = buffer.size

  def resolution_=(value: (Int, Int)) =
    if (supportedResolutions.contains(value) && (buffer.size = value)) {
      val (w, h) = value
      owner.onScreenResolutionChange(w, h)
      true
    }
    else false

  def set(col: Int, row: Int, s: String) = if (col < buffer.width && (col >= 0 || -col < s.length)) {
    // Make sure the string isn't longer than it needs to be, in particular to
    // avoid sending too much data to our clients.
    val (x, truncated) =
      if (col < 0) (0, s.substring(-col))
      else (col, s.substring(0, s.length min buffer.width))
    if (buffer.set(x, row, truncated))
      owner.onScreenSet(x, row, truncated)
  }

  def fill(col: Int, row: Int, w: Int, h: Int, c: Char) =
    if (buffer.fill(col, row, w, h, c))
      owner.onScreenFill(col, row, w, h, c)

  def copy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) =
    if (buffer.copy(col, row, w, h, tx, ty))
      owner.onScreenCopy(col, row, w, h, tx, ty)

  // ----------------------------------------------------------------------- //

  def load(nbt: NBTTagCompound) = {
    buffer.readFromNBT(nbt.getCompoundTag("buffer"))
  }

  def save(nbt: NBTTagCompound) = {
    val nbtBuffer = new NBTTagCompound
    buffer.writeToNBT(nbtBuffer)
    nbt.setCompoundTag("buffer", nbtBuffer)
  }
}

object Screen {

  trait Environment extends Node {
    final val screen = new Screen(this)

    // ----------------------------------------------------------------------- //

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

    // ----------------------------------------------------------------------- //

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

    // ----------------------------------------------------------------------- //

    def onScreenResolutionChange(w: Int, h: Int) =
      network.foreach(_.sendToVisible(this, "computer.signal", "screen_resized", w, h))

    def onScreenSet(col: Int, row: Int, s: String) {}

    def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char) {}

    def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {}
  }

}