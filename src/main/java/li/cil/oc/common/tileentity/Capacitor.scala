package li.cil.oc.common.tileentity

import li.cil.oc.api.network.Visibility
import li.cil.oc.{Blocks, Settings, api}
import net.minecraftforge.common.ForgeDirection
import scala.collection.convert.WrapAsScala._

class Capacitor extends Environment with PassiveNode {
  // Start with maximum theoretical capacity, gets reduced after validation.
  // This is done so that we don't lose energy while loading.
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector(maxCapacity).
    create()

  override def canUpdate = false

  override def validate() {
    super.validate()
    world.scheduleBlockUpdateFromLoad(x, y, z, Blocks.capacitor.parent.blockID, 0, 0)
  }

  override def invalidate() {
    super.invalidate()
    if (isServer) {
      indirectNeighbors.map(coordinate => {
        val (nx, ny, nz) = coordinate
        world.getBlockTileEntity(nx, ny, nz)
      }).collect {
        case capacitor: Capacitor => capacitor.recomputeCapacity()
      }
    }
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    if (isServer) {
      // Avoid triggering a chunk load...
      val in = indirectNeighbors
      world.loadedTileEntityList.collect {
        case capacitor: Capacitor if in.exists(coordinate => {
          val (nx, ny, nz) = coordinate
          nx == capacitor.x && ny == capacitor.y && nz == capacitor.z
        }) => capacitor.recomputeCapacity()
      }
    }
  }

  def recomputeCapacity(updateSecondGradeNeighbors: Boolean = false) {
    world.activeChunkSet
    node.setLocalBufferSize(
      Settings.get.bufferCapacitor +
        Settings.get.bufferCapacitorAdjacencyBonus * ForgeDirection.VALID_DIRECTIONS.count(side => {
          world.getBlockTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ) match {
            case capacitor: Capacitor => true
            case _ => false
          }
        }) +
        Settings.get.bufferCapacitorAdjacencyBonus / 2 * indirectNeighbors.count {
          case (nx, ny, nz) => world.getBlockTileEntity(nx, ny, nz) match {
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
