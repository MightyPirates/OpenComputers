package li.cil.oc.common.component

import li.cil.oc.api
import li.cil.oc.api.Persistable
import li.cil.oc.api.network.{Message, Visibility}
import li.cil.oc.common.{tileentity, component}
import li.cil.oc.util.TextBuffer
import net.minecraft.nbt.NBTTagCompound

class Screen(val owner: Screen.Environment, val maxResolution: (Int, Int)) extends Persistable {
  private val buffer = new TextBuffer(maxResolution)

  // ----------------------------------------------------------------------- //

  def text = buffer.toString

  def lines = buffer.buffer

  def resolution = buffer.size

  def resolution_=(value: (Int, Int)) = {
    val (w, h) = value
    val (mw, mh) = maxResolution
    if (w <= mw && h <= mh && (buffer.size = value)) {
      owner.onScreenResolutionChange(w, h)
      true
    }
    else false
  }

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

  override def load(nbt: NBTTagCompound) = {
    buffer.readFromNBT(nbt.getCompoundTag("oc.screen"))
  }

  override def save(nbt: NBTTagCompound) = {
    val screenNbt = new NBTTagCompound
    buffer.writeToNBT(screenNbt)
    nbt.setCompoundTag("oc.screen", screenNbt)
  }
}

object Screen {

  trait Environment extends tileentity.Environment with tileentity.Persistable {
    val node = api.Network.createComponent(api.Network.createNode(this, "screen", Visibility.Network))

    final val instance = new component.Screen(this, maxResolution)

    protected def maxResolution: (Int, Int)

    // ----------------------------------------------------------------------- //

    override def onMessage(message: Message) = {
      message.data match {
        case Array(w: Integer, h: Integer) if message.name == "screen.resolution=" =>
          Array(Boolean.box(instance.resolution = (w, h)))
        case Array() if message.name == "screen.resolution" => {
          val (w, h) = instance.resolution
          Array(Int.box(w), Int.box(h))
        }
        case Array() if message.name == "screen.maxResolution" =>
          val (w, h) = instance.maxResolution
          Array(Int.box(w), Int.box(h))
        case Array(x: Integer, y: Integer, value: String) if message.name == "screen.set" =>
          instance.set(x, y, value)
          Array(Boolean.box(true))
        case Array(x: Integer, y: Integer, w: Integer, h: Integer, value: Character) if message.name == "screen.fill" =>
          instance.fill(x, y, w, h, value)
          Array(Boolean.box(true))
        case Array(x: Integer, y: Integer, w: Integer, h: Integer, tx: Integer, ty: Integer) if message.name == "screen.copy" =>
          instance.copy(x, y, w, h, tx, ty)
          Array(Boolean.box(true))
        case _ => super.onMessage(message)
      }
    }

    // ----------------------------------------------------------------------- //

    override def load(nbt: NBTTagCompound) = {
      super.load(nbt)
      if (node != null) node.load(nbt)
      instance.load(nbt)
    }

    override def save(nbt: NBTTagCompound) = {
      super.save(nbt)
      if (node != null) node.save(nbt)
      instance.save(nbt)
    }

    // ----------------------------------------------------------------------- //

    def onScreenResolutionChange(w: Int, h: Int) {
      if (node != null && node.network != null)
        node.network.sendToVisible(node, "computer.signal", "screen_resized", Int.box(w), Int.box(h))
    }

    def onScreenSet(col: Int, row: Int, s: String) {}

    def onScreenFill(col: Int, row: Int, w: Int, h: Int, c: Char) {}

    def onScreenCopy(col: Int, row: Int, w: Int, h: Int, tx: Int, ty: Int) {}
  }

}