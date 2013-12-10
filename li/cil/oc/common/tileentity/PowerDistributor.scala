package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

class PowerDistributor extends Environment with SidedEnvironment with PowerInformation with Analyzable {
  private val nodes = Array.fill(6)(api.Network.newNode(this, Visibility.Network).withConnector().create())

  val node = null

  def sidedNode(side: ForgeDirection) = nodes(side.ordinal)

  def canConnect(side: ForgeDirection) = true

  var globalBuffer, globalBufferSize = 0.0

  // ----------------------------------------------------------------------- //

  override def onAnalyze(stats: NBTTagCompound, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = null

  override def updateEntity() {
    super.updateEntity()
    if (isServer && world.getWorldTime % Settings.get.tickFrequency == 0) {
      // Yeeeeah, so that just happened... it's not a beauty, but it works.
      nodes(0).network.synchronized(nodes(1).network.synchronized(nodes(2).network.synchronized(nodes(3).network.synchronized(nodes(4).network.synchronized(nodes(5).network.synchronized {
        var sumBuffer, sumSize = 0.0
        for (node <- nodes if isPrimary(node)) {
          sumBuffer += node.globalBuffer
          sumSize += node.globalBufferSize
        }
        if (sumSize > 0) {
          val ratio = sumBuffer / sumSize
          for (node <- nodes if isPrimary(node)) {
            node.changeBuffer(node.globalBufferSize * ratio - node.globalBuffer)
          }
        }
        globalBuffer = sumBuffer
        globalBufferSize = sumSize
      })))))
      updatePowerInformation()
    }
  }

  private def isPrimary(connector: Connector) = nodes(nodes.indexWhere(_.network == connector.network)) == connector

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    nbt.getTagList(Settings.namespace + "connector").iterator[NBTTagCompound].zip(nodes).foreach {
      case (connectorNbt, connector) => connector.load(connectorNbt)
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setNewTagList(Settings.namespace + "connector", nodes.map(connector => {
      val connectorNbt = new NBTTagCompound()
      connector.save(connectorNbt)
      connectorNbt
    }))
  }
}
