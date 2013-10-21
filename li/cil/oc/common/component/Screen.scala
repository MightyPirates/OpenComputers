package li.cil.oc.common.component

import li.cil.oc.api.Persistable
import li.cil.oc.api.network.{Component, Message, Visibility}
import li.cil.oc.util.TextBuffer
import net.minecraft.nbt.NBTTagCompound
import li.cil.oc.common.component

class Screen(val owner: Screen.Environment, val resolutions: List[(Int, Int)]) extends Persistable {
  private val buffer = new TextBuffer(resolutions.head._1, resolutions.head._2)

  // ----------------------------------------------------------------------- //

  def text = buffer.toString

  def lines = buffer.buffer

  def resolution = buffer.size

  def resolution_=(value: (Int, Int)) =
    if (resolutions.contains(value) && (buffer.size = value)) {
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

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    buffer.readFromNBT(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
    buffer.writeToNBT(nbt)
  }
}

object Screen {

  trait Environment extends Component {
    final val instance = new component.Screen(this, resolutions)

    override val name = "screen"

    override val visibility = Visibility.Network

    protected def resolutions: List[(Int, Int)]

    // ----------------------------------------------------------------------- //

    override def receive(message: Message): Option[Array[Any]] = super.receive(message).orElse {
      message.data match {
        case Array(w: Int, h: Int) if message.name == "screen.resolution=" =>
          result(instance.resolution = (w, h))
        case Array() if message.name == "screen.resolution" => {
          val (w, h) = instance.resolution
          result(w, h)
        }
        case Array() if message.name == "screen.resolutions" =>
          result(instance.resolutions: _*)
        case Array(x: Int, y: Int, value: String) if message.name == "screen.set" =>
          instance.set(x, y, value); None
        case Array(x: Int, y: Int, w: Int, h: Int, value: Char) if message.name == "screen.fill" =>
          instance.fill(x, y, w, h, value); None
        case Array(x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int) if message.name == "screen.copy" =>
          instance.copy(x, y, w, h, tx, ty); None
        case _ => None
      }
    }

    // ----------------------------------------------------------------------- //

    override abstract def readFromNBT(nbt: NBTTagCompound) = {
      super.readFromNBT(nbt)

      instance.readFromNBT(nbt.getCompoundTag("screen"))
    }

    override abstract def writeToNBT(nbt: NBTTagCompound) = {
      super.writeToNBT(nbt)

      val screenNbt = new NBTTagCompound
      instance.writeToNBT(screenNbt)
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