package li.cil.oc.common.tileentity

import li.cil.oc.api
import li.cil.oc.api.network.{Visibility, Node}
import li.cil.oc.server.driver
import net.minecraftforge.common.ForgeDirection

class Adapter extends Rotatable with Node {
  val name = "adapter"

  val visibility = Visibility.None

  private val blocks = Array.fill[Option[(Node, api.driver.Block)]](6)(None)

  override protected def onConnect() {
    super.onConnect()
    neighborChanged()
  }

  def neighborChanged() {
    for (d <- ForgeDirection.VALID_DIRECTIONS) {
      val (x, y, z) = (xCoord + d.offsetX, yCoord + d.offsetY, zCoord + d.offsetZ)
      driver.Registry.driverFor(worldObj, x, y, z) match {
        case Some(newDriver) => blocks(d.ordinal()) match {
          case Some((node, driver)) =>
            if (newDriver != driver) {
              // This is... odd.
              network.foreach(_.disconnect(this, node))
              val newNode = newDriver.node(worldObj, x, y, z)
              network.foreach(_.connect(this, newNode))
              blocks(d.ordinal()) = Some((newNode, newDriver))
            } // else: the more things change, the more they stay the same.
          case _ =>
            // A challenger appears.
            val node = newDriver.node(worldObj, x, y, z)
            network.foreach(_.connect(this, node))
            blocks(d.ordinal()) = Some((node, newDriver))
        }
        case _ => blocks(d.ordinal()) match {
          case Some((node, driver)) =>
            // We had something there, but it's gone now...
            blocks(d.ordinal()) = None
            network.foreach(_.disconnect(this, node))
          case _ => // Nothing before, nothing now.
        }
      }
    }
  }
}
