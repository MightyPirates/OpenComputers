package li.cil.oc.common.tileentity

import li.cil.oc.api.Network
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.block

class Cable extends Environment {
  val node = Network.newNode(this, Visibility.None).create()

  def neighbors = block.Cable.neighbors(worldObj, xCoord, yCoord, zCoord)

  override def getRenderBoundingBox =
    block.Cable.
      bounds(worldObj, xCoord, yCoord, zCoord).
      offset(xCoord, yCoord, zCoord)
}
