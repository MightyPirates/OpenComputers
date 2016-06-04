package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Constants
import li.cil.oc.Constants.DeviceInfo.DeviceAttribute
import li.cil.oc.Constants.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.Visibility
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._

class Capacitor extends traits.Environment with DeviceInfo {
  // Start with maximum theoretical capacity, gets reduced after validation.
  // This is done so that we don't lose energy while loading.
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector(maxCapacity).
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Power,
    DeviceAttribute.Description -> "Battery",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "CapBank3x",
    DeviceAttribute.Capacity -> maxCapacity.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  override def canUpdate = false

  override def dispose() {
    super.dispose()
    if (isServer) {
      indirectNeighbors.map(coordinate => {
        val (nx, ny, nz) = coordinate
        if (world.blockExists(nx, ny, nz)) world.getTileEntity(nx, ny, nz)
        else null
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
          val (nx, ny, nz) = (x + side.offsetX, y + side.offsetY, z + side.offsetZ)
          world.blockExists(nx, ny, nz) && (world.getTileEntity(nx, ny, nz) match {
            case capacitor: Capacitor => true
            case _ => false
          })
        }) +
        Settings.get.bufferCapacitorAdjacencyBonus / 2 * indirectNeighbors.count {
          case (nx, ny, nz) => world.blockExists(nx, ny, nz) && (world.getTileEntity(nx, ny, nz) match {
            case capacitor: Capacitor =>
              if (updateSecondGradeNeighbors) {
                capacitor.recomputeCapacity()
              }
              true
            case _ => false
          })
        })
  }

  private def indirectNeighbors = ForgeDirection.VALID_DIRECTIONS.map(side => (x + side.offsetX * 2, y + side.offsetY * 2, z + side.offsetZ * 2))

  private def maxCapacity = Settings.get.bufferCapacitor + Settings.get.bufferCapacitorAdjacencyBonus * 9
}
