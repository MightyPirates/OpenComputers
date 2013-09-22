package li.cil.oc.server.components

import li.cil.oc.common.components.Screen
import net.minecraft.nbt.NBTTagCompound

/**
 * Graphics cards are what we use to render text to screens. 
 */
class GraphicsCard(val nbt: NBTTagCompound) extends IComponent {
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