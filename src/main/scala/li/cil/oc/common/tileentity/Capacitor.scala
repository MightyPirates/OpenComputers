package li.cil.oc.common.tileentity

import li.cil.oc.api.network.{Node, Visibility}
import li.cil.oc.{Settings, api}
import net.minecraftforge.common.util.ForgeDirection

class Capacitor extends traits.Environment {
  // Start with maximum theoretical capacity, gets reduced after validation.
  // This is done so that we don't lose energy while loading.
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector(maxCapacity).
    create()

  // ----------------------------------------------------------------------- //

  override def canUpdate = false

  override protected def dispose() {
    super.dispose()
    if (isServer) {
      indirectNeighbors.map(coordinate => {
        val (nx, ny, nz) = coordinate
        world.getTileEntity(nx, ny, nz)
      }).collect {
        case capacitor: Capacitor => capacitor.recomputeCapacity()
      }
    }
  }

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      recomputeCapacity(updateSecondGradeNeighbors = true)
    }
  }

  // ----------------------------------------------------------------------- //

  def recomputeCapacity(updateSecondGradeNeighbors: Boolean = false) {
    node.setLocalBufferSize(
      Settings.get.bufferCapacitor +
        Settings.get.bufferCapacitorAdjacencyBonus * ForgeDirection.VALID_DIRECTIONS.count(side => {
          world.getTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ) match {
            case capacitor: Capacitor => true
            case _ => false
          }
        }) +
        Settings.get.bufferCapacitorAdjacencyBonus / 2 * indirectNeighbors.count {
          case (nx, ny, nz) => world.getTileEntity(nx, ny, nz) match {
            case capacitor: Capacitor =>
              if (updateSecondGradeNeighbors) {
                capacitor.recomputeCapacity()
              }
              true
            case _ => false
          }
        })
  }

  private def indirectNeighbors = ForgeDirection.VALID_DIRECTIONS.map(side => (x + side.offsetX * 2, y + side.offsetY * 2, z + side.offsetZ * 2))

  private def maxCapacity = Settings.get.bufferCapacitor + Settings.get.bufferCapacitorAdjacencyBonus * 9
}
