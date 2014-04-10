package li.cil.oc.common.tileentity

import li.cil.oc.api.network.Visibility
import li.cil.oc.{api, common}

class Cable extends traits.Environment with traits.NotAnalyzable {
  val node = api.Network.newNode(this, Visibility.None).create()

  override def canUpdate = false

  override def getRenderBoundingBox = common.block.Cable.bounds(world, x, y, z).offset(x, y, z)

  // ----------------------------------------------------------------------- //
  // Immibis Microblock support.

  val ImmibisMicroblocks_TransformableTileEntityMarker = null

  def ImmibisMicroblocks_isSideOpen(side: Int) = true

  def ImmibisMicroblocks_onMicroblocksChanged() {
    api.Network.joinOrCreateNetwork(this)
  }
}
