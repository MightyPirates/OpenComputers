package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api.{Network, network}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.Persistable
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.INetworkManager
import net.minecraft.network.packet.Packet132TileEntityData
import scala.math.ScalaNumber

abstract class Environment extends net.minecraft.tileentity.TileEntity with TileEntity with network.Environment with Persistable {
  def world = getWorldObj

  def x = xCoord

  def y = yCoord

  def z = zCoord

  def block = getBlockType

  private var addedToNetwork = false

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (!addedToNetwork) {
      addedToNetwork = true
      Network.joinOrCreateNetwork(this)
    }
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    Option(node).foreach(_.remove)
  }

  override def invalidate() {
    super.invalidate()
    Option(node).foreach(_.remove)
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    load(nbt)
    if (node != null && node.host == this) node.load(nbt.getCompoundTag(Settings.namespace + "node"))
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    save(nbt)
    if (node != null && node.host == this) nbt.setNewCompoundTag(Settings.namespace + "node", node.save)
  }

  // ----------------------------------------------------------------------- //

  override def getDescriptionPacket = {
    val nbt = new NBTTagCompound()
    writeToNBTForClient(nbt)
    if (nbt.hasNoTags) null else new Packet132TileEntityData(x, y, z, -1, nbt)
  }

  override def onDataPacket(manager: INetworkManager, packet: Packet132TileEntityData) {
    readFromNBTForClient(packet.data)
  }

  // ----------------------------------------------------------------------- //

  def onMessage(message: network.Message) {}

  def onConnect(node: network.Node) {}

  def onDisconnect(node: network.Node) {}

  // ----------------------------------------------------------------------- //

  final protected def result(args: Any*): Array[AnyRef] = {
    def unwrap(arg: Any): AnyRef = arg match {
      case x: ScalaNumber => x.underlying
      case x => x.asInstanceOf[AnyRef]
    }
    Array(args map unwrap: _*)
  }
}
