package li.cil.oc.server.components

import li.cil.oc.common.util.TextBuffer

import net.minecraft.nbt.NBTTagCompound

/**
 * Graphics cards are what we use to render text to screens. They have an
 * internal text buffer that can be manipulated from the Lua side via the
 * GPU driver. These changes are forwarded to any monitors the card is bound
 * to, if any. Note that the screen component on the server does not have an
 * internal state. It merely generates packets to be sent to the client, whose
 * screen component in turn has a state similar to a graphics card which is
 * used by the GUI to display the text in the buffer.
 *
 * TODO minimize NBT updates, i.e. only write what really changed?
 */
class GraphicsCard(val nbt: NBTTagCompound) extends IComponent {
  id = 1
  readFromNBT()

  val resolutions = List(List(40, 24), List(80, 24))

  private val buffer = new TextBuffer(80, 24)

  var screen: Option[Screen] = None

  def resolution = buffer.size

  def resolution_=(value: (Int, Int)) =
    if (resolutions.contains(value)) {
      buffer.size = value
      screen.foreach(_.resolution = value)
      writeToNBT()
    }
    else throw new IllegalArgumentException("unsupported resolution")

  def set(x: Int, y: Int, s: String): Unit = {
    // Make sure the string isn't longer than it needs to be, in particular to
    // avoid sending too much data to our clients.
    val truncated = s.substring(0, buffer.width min s.length)
    buffer.set(x, y, truncated)
    screen.foreach(_.set(x, y, truncated))
    writeToNBT()
  }

  def fill(x: Int, y: Int, w: Int, h: Int, c: Char) = {
    buffer.fill(x, y, w, h, c)
    screen.foreach(_.fill(x, y, w, h, c))
    writeToNBT()
  }

  def copy(x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int) = {
    buffer.copy(x, y, w, h, tx, ty)
    screen.foreach(_.copy(x, y, w, h, tx, ty))
    writeToNBT()
  }

  def bind(value: Option[Screen]): Unit = {
    screen = value
    screen.foreach(screen => {
      screen.resolution = resolution
      fill(0, 0, buffer.width, buffer.height, ' ')
    })
    writeToNBT()
  }

  def readFromNBT(): Unit = {
    // A new instance has no data written to its NBT tag compound.
    if (!nbt.hasKey("monitor.x")) return
    val x = nbt.getInteger("monitor.x")
    val y = nbt.getInteger("monitor.y")
    val z = nbt.getInteger("monitor.z")
    // TODO get tile entity in world, get its screen component
    buffer.readFromNBT(nbt)
  }

  def writeToNBT(): Unit = {
    if (screen.isDefined) {
      val owner = screen.get.owner
      nbt.setInteger("monitor.x", owner.xCoord)
      nbt.setInteger("monitor.y", owner.yCoord)
      nbt.setInteger("monitor.z", owner.zCoord)
    }
    buffer.writeToNBT(nbt)
  }
}