package li.cil.oc.common.tileentity

import li.cil.oc.api.network.{Node, Visibility}
import li.cil.oc.common.EventHandler
import li.cil.oc.{Settings, api}
import net.minecraftforge.common.util.ForgeDirection
import scala.collection.convert.WrapAsScala._

class Capacitor extends traits.Environment {
  // Start with maximum theoretical capacity, gets reduced after validation.
  // This is done so that we don't lose energy while loading.
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector(maxCapacity).
    create()

  // ----------------------------------------------------------------------- //

  override def canUpdate = false

  override def validate() {
    super.validate()
    EventHandler.schedule(this)
  }

  override def invalidate() {
    super.invalidate()
    if (isServer) {
      indirectNeighbors.map(coordinate => {
        val (nx, ny, nz) = coordinate
        world.getTileEntity(nx, ny, nz)
      }).collect {
        case capacitor: Capacitor => capacitor.recomputeCapacity()
      }
    }
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    if (isServer) {
      // Avoid triggering a chunk load...
      // TODO I'm pretty sure this actually isn't necessary.
      val in = indirectNeighbors
      world.loadedTileEntityList.collect {
        case capacitor: Capacitor if in.exists(coordinate => {
          val (nx, ny, nz) = coordinate
          nx == capacitor.x && ny == capacitor.y && nz == capacitor.z
        }) => capacitor.recomputeCapacity()
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
