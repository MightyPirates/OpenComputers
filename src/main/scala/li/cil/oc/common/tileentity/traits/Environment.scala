package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.api.network
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.common.EventHandler
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.nbt.CompoundNBT
import net.minecraft.util.Direction
import net.minecraftforge.client.model.data.IModelData
import net.minecraftforge.client.model.data.ModelProperty

trait Environment extends TileEntity with network.Environment with network.EnvironmentHost with IModelData {
  protected var isChangeScheduled = false

  override def world = getLevel

  override def xPosition = x + 0.5

  override def yPosition = y + 0.5

  override def zPosition = z + 0.5

  override def markChanged() = if (this.isInstanceOf[Tickable]) isChangeScheduled = true else getLevel.blockEntityChanged(getBlockPos, this)

  protected def isConnected = node != null && node.address != null && node.network != null

  // ----------------------------------------------------------------------- //

  override protected def initialize() {
    super.initialize()
    if (isServer) {
      EventHandler.scheduleServer(this)
    }
  }

  override def updateEntity() {
    super.updateEntity()
    if (isChangeScheduled) {
      getLevel.blockEntityChanged(getBlockPos, this)
      isChangeScheduled = false
    }
  }

  override def dispose() {
    super.dispose()
    if (isServer) {
      Option(node).foreach(_.remove)
      this match {
        case sidedEnvironment: SidedEnvironment => for (side <- Direction.values) {
          Option(sidedEnvironment.sidedNode(side)).foreach(_.remove())
        }
        case _ =>
      }
    }
  }

  // ----------------------------------------------------------------------- //

  private final val NodeTag = Settings.namespace + "node"

  override def loadForServer(nbt: CompoundNBT) {
    super.loadForServer(nbt)
    if (node != null && node.host == this) {
      node.loadData(nbt.getCompound(NodeTag))
    }
  }

  override def saveForServer(nbt: CompoundNBT) {
    super.saveForServer(nbt)
    if (node != null && node.host == this) {
      nbt.setNewCompoundTag(NodeTag, node.saveData)
    }
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: network.Message) {}

  override def onConnect(node: network.Node) {}

  override def onDisconnect(node: network.Node) {
    if (node == this.node) node match {
      case connector: Connector =>
        // Set it to zero to push all energy into other nodes, to
        // avoid energy loss when removing nodes. Set it back to the
        // original value though, as there are cases where the node
        // is re-used afterwards, without re-adjusting its buffer size.
        var bufferSize = connector.localBufferSize()
        connector.setLocalBufferSize(0)
        connector.setLocalBufferSize(bufferSize)
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  protected def result(args: Any*) = li.cil.oc.util.ResultWrapper.result(args: _*)

  // ----------------------------------------------------------------------- //

  @Deprecated
  override def getModelData() = this

  @Deprecated
  override def hasProperty(prop: ModelProperty[_]) = false

  @Deprecated
  override def getData[T](prop: ModelProperty[T]): T = null.asInstanceOf[T]

  @Deprecated
  override def setData[T](prop: ModelProperty[T], value: T): T = null.asInstanceOf[T]
}
