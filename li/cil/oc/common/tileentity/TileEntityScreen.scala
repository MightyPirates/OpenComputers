package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher._

import li.cil.oc.client.components.{ Screen => ClientScreen }
import li.cil.oc.server.components.{ Screen => ServerScreen }
import net.minecraft.tileentity.TileEntity

class TileEntityScreen(isClient: Boolean) extends TileEntity {
  def this() = this(false)

  val component =
    if (isClient) new ClientScreen(this)
    else new ServerScreen(this)

  @SideOnly(Side.CLIENT)
  def updateGui(value: () => String): Unit = {
    // TODO if GUI is open, call value() to get the new display string and show it
  }

  // TODO open GUI on right click
}