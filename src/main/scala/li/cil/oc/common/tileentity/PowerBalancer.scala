package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api.network.{Connector, SidedEnvironment}
import net.minecraftforge.common.util.ForgeDirection

trait PowerBalancer extends PowerInformation with SidedEnvironment {
  var globalBuffer, globalBufferSize = 0.0

  override def updateEntity() {
    super.updateEntity()
    if (isServer && world.getWorldTime % Settings.get.tickFrequency == 0) {
      val nodes = ForgeDirection.VALID_DIRECTIONS.view.map(sidedNode(_) match {
        case connector: Connector => connector
        case _ => null
      })
      def isPrimary(connector: Connector) = connector != null && nodes(nodes.indexWhere(_.network == connector.network)) == connector
      def network(connector: Connector) = if (connector != null) connector.network else this
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
                }
              }
            }
          }
        }
      }
      updatePowerInformation()
    }
  }
}
