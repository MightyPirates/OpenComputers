package li.cil.oc.common.tileentity

import li.cil.oc.api.network.{Analyzable, Visibility}
import li.cil.oc.common.EventHandler
import li.cil.oc.{api, common}
import net.minecraft.entity.player.EntityPlayer

class Cable extends Environment with Analyzable with PassiveNode {
  val node = api.Network.newNode(this, Visibility.None).create()

  def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = null

  def neighbors = common.block.Cable.neighbors(world, x, y, z)

  override def canUpdate = false

  override def validate() {
    super.validate()
    EventHandler.schedule(this)
  }

  override def getRenderBoundingBox = common.block.Cable.bounds(world, x, y, z).offset(x, y, z)
}
