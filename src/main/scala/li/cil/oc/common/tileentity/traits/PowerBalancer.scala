package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.SidedEnvironment
import net.minecraft.util.EnumFacing

trait PowerBalancer extends PowerInformation with SidedEnvironment with Tickable {
  var globalBuffer, globalBufferSize = 0.0

  protected def isConnected: Boolean

  override def updateEntity() {
    super.updateEntity()
    if (isServer && isConnected && getWorld.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      val nodes = connectors
      def network(connector: Connector) = if (connector != null && connector.network != null) connector.network else this
      // Yeeeeah, so that just happened... it's not a beauty, but it works. This
      // is necessary because power in networks can be updated asynchronously,
      // i.e. in separate threads (e.g. to allow screens to consume energy when
      // they change, which usually happens in a computers executor thread).
      // This multi-lock only happens in the main server thread, though, so we
      // don't have to fear deadlocks. I think.
      network(nodes(0)).synchronized {
        network(nodes(1)).synchronized {
          network(nodes(2)).synchronized {
            network(nodes(3)).synchronized {
              network(nodes(4)).synchronized {
                network(nodes(5)).synchronized {
                  val (sumBuffer, sumSize) = distribute()
                  if (sumSize > 0) {
                    val ratio = sumBuffer / sumSize
                    for (node <- connectors if isPrimary(node)) {
                      node.changeBuffer(node.globalBufferSize * ratio - node.globalBuffer)
                    }
                  }
                  globalBuffer = sumBuffer
                  globalBufferSize = sumSize
                }
              }
            }
          }
        }
      }
      updatePowerInformation()
    }
  }

  protected def distribute() = {
    var sumBuffer, sumSize = 0.0
    for (node <- connectors if isPrimary(node)) {
      sumBuffer += node.globalBuffer
      sumSize += node.globalBufferSize
    }
    (sumBuffer, sumSize)
  }

  private def connectors = EnumFacing.values.view.map(sidedNode(_) match {
    case connector: Connector => connector
    case _ => null
  })

  private def isPrimary(connector: Connector) = {
    val nodes = connectors
    connector != null && nodes(nodes.indexWhere(node => node != null && node.network == connector.network)) == connector
  }
}
