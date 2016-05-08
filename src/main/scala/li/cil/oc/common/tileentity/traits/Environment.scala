package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.api.network
import li.cil.oc.api.network.Connector
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.SidedEnvironment
import li.cil.oc.common.EventHandler
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.server.network.Network
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.common.Optional

@Injectable.Interface(value = "appeng.api.movable.IMovableTile", modid = Mods.IDs.AppliedEnergistics2)
trait Environment extends TileEntity with network.Environment with network.EnvironmentHost {
  protected var isChangeScheduled = false

  override def xPosition = x + 0.5

  override def yPosition = y + 0.5

  override def zPosition = z + 0.5

  override def markChanged() = if (canUpdate) isChangeScheduled = true else world.markChunkDirty(getPos, this)

  protected def isConnected = node.address != null && node.network != null

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
      world.markChunkDirty(getPos, this)
      isChangeScheduled = false
    }
  }

  override def dispose() {
    super.dispose()
    if (isServer) {
      if (moving && this.isInstanceOf[Computer]) {
        this match {
          case env: SidedEnvironment =>
            for (side <- EnumFacing.values) {
              val npos = position.offset(side)
              Network.getNetworkNode(world.getTileEntity(npos), side.getOpposite) match {
                case neighbor: Node if env.sidedNode(side) != null => env.sidedNode(side).disconnect(neighbor)
                case _ => // No neighbor node.
              }
            }
          case env =>
            for (side <- EnumFacing.values) {
              val npos = position.offset(side)
              Network.getNetworkNode(world.getTileEntity(npos), side.getOpposite) match {
                case neighbor: Node if env.node != null => env.node.disconnect(neighbor)
                case _ => // No neighbor node.
              }
            }
        }
      }
      else {
        Option(node).foreach(_.remove)
        this match {
          case sidedEnvironment: SidedEnvironment => for (side <- EnumFacing.values) {
            Option(sidedEnvironment.sidedNode(side)).foreach(_.remove())
          }
          case _ =>
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    if (node != null && node.host == this) {
      node.load(nbt.getCompoundTag(Settings.namespace + "node"))
    }
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    if (node != null && node.host == this) {
      nbt.setNewCompoundTag(Settings.namespace + "node", node.save)
    }
  }

  // ----------------------------------------------------------------------- //

  override def onMessage(message: network.Message) {}

  override def onConnect(node: network.Node) {}

  override def onDisconnect(node: network.Node) {
    if (node == this.node) node match {
      case connector: Connector => connector.setLocalBufferSize(0)
      case _ =>
    }
  }

  // ----------------------------------------------------------------------- //

  protected var moving = false

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  def prepareToMove(): Boolean = {
    moving = true
    true
  }

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  def doneMoving(): Unit = {
    moving = false
    Network.joinOrCreateNetwork(this)
    world.notifyBlockUpdate(getPos)
  }

  // ----------------------------------------------------------------------- //

  protected def result(args: Any*) = li.cil.oc.util.ResultWrapper.result(args: _*)
}
