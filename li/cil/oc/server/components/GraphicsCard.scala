package li.cil.oc.server.components

import li.cil.oc.common.components.Screen
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
  id =
    if (nbt.hasKey("componentId")) nbt.getInteger("id")
    else 1

  override def id_=(value: Int) = {
    super.id = value
    nbt.setInteger("componentId", value)
  }

  val supportedResolutions = List(List(40, 24), List(80, 24))

  def resolution(screen: Screen) = screen.resolution

  def resolution(screen: Screen, value: (Int, Int)) =
    if (supportedResolutions.contains(value))
      screen.resolution = value

  def set(screen: Screen, x: Int, y: Int, s: String): Unit =
    screen.set(x, y, s)

  def fill(screen: Screen, x: Int, y: Int, w: Int, h: Int, c: Char) =
    screen.fill(x, y, w, h, c)

  def copy(screen: Screen, x: Int, y: Int, w: Int, h: Int, tx: Int, ty: Int) =
    screen.copy(x, y, w, h, tx, ty)
}