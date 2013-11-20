package li.cil.oc.common.tileentity

import li.cil.oc.api.network.{Analyzable, Visibility}
import li.cil.oc.{api, common}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

class Cable extends Environment with Analyzable {
  val node = api.Network.newNode(this, Visibility.None).create()

  def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = null

  def neighbors = common.block.Cable.neighbors(world, x, y, z)

  // TODO use one block tick for net join instead of constant updates
//  override def canUpdate = false

  override def getRenderBoundingBox = common.block.Cable.bounds(world, x, y, z).offset(x, y, z)
}
