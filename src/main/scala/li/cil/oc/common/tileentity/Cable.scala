package li.cil.oc.common.tileentity

import li.cil.oc.api.network.Visibility
import li.cil.oc.{api, common}

class Cable extends traits.Environment with traits.NotAnalyzable with traits.ImmibisMicroblock {
  val node = api.Network.newNode(this, Visibility.None).create()

  override def canUpdate = false

  override def getRenderBoundingBox = common.block.Cable.bounds(world, x, y, z).offset(x, y, z)
}
