package li.cil.oc.common.tileentity

import li.cil.oc.api.Network
import li.cil.oc.api.network.Visibility
import li.cil.oc.common

class Cable extends Environment {
  val node = Network.newNode(this, Visibility.None).create()

  def neighbors = common.block.Cable.neighbors(world, x, y, z)

  override def getRenderBoundingBox = common.block.Cable.bounds(world, x, y, z).offset(x, y, z)
}
