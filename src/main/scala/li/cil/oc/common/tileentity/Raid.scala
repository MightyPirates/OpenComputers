package li.cil.oc.common.tileentity

import java.util.UUID

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.fs.Label
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.item.data.DriveData
import li.cil.oc.common.Slot
import li.cil.oc.common.item.data.NodeData
import li.cil.oc.server.component.FileSystem
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class Raid extends TileEntity(null) with traits.Environment with traits.Inventory with traits.Rotatable with Analyzable {
  val node = api.Network.newNode(this, Visibility.None).create()

  var filesystem: Option[FileSystem] = None

  val label = new RaidLabel()

  // Used on client side to check whether to render disk activity indicators.
  var lastAccess = 0L

  // For client side rendering.
  val presence = Array.fill(getContainerSize)(false)

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: PlayerEntity, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = Array(filesystem.map(_.node).orNull)

  // ----------------------------------------------------------------------- //

  override def getContainerSize = 3

  override def getMaxStackSize = 1

  override def canPlaceItem(slot: Int, stack: ItemStack) = Option(Driver.driverFor(stack, getClass)) match {
    case Some(driver) => driver.slot(stack) == Slot.HDD
    case _ => false
  }

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    if (isServer) this.synchronized {
      ServerPacketSender.sendRaidChange(this)
      tryCreateRaid(UUID.randomUUID().toString)
    }
  }

  override def setChanged() {
    super.setChanged()
    // Makes the implementation of the comparator output easier.
    items.map(!_.isEmpty).copyToArray(presence)
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (isServer) this.synchronized {
      ServerPacketSender.sendRaidChange(this)
      filesystem.foreach(fs => {
        fs.fileSystem.close()
        fs.fileSystem.list("/").foreach(fs.fileSystem.delete)
        fs.saveData(new CompoundNBT()) // Flush buffered fs.
        fs.node.remove()
        filesystem = None
      })
    }
  }

  def tryCreateRaid(id: String) {
    if (items.count(!_.isEmpty) == items.length && filesystem.fold(true)(fs => fs.node == null || fs.node.address != id)) {
      filesystem.foreach(fs => if (fs.node != null) fs.node.remove())
      items.foreach(fsStack => {
        val drive = new DriveData(fsStack)
        drive.lockInfo = ""
        drive.isUnmanaged = false
        drive.saveData(fsStack)
      })
      val fs = api.FileSystem.asManagedEnvironment(
        api.FileSystem.fromSaveDirectory(id, wipeDisksAndComputeSpace, Settings.get.bufferChanges),
        label, this, Settings.resourceDomain + ":hdd_access", 6).
        asInstanceOf[FileSystem]
      val nbtToSetAddress = new CompoundNBT()
      nbtToSetAddress.putString(NodeData.AddressTag, id)
      fs.node.loadData(nbtToSetAddress)
      fs.node.setVisibility(Visibility.Network)
      // Ensure we're in a network before connecting the raid fs.
      api.Network.joinNewNetwork(node)
      node.connect(fs.node)
      filesystem = Option(fs)
    }
  }

  private def wipeDisksAndComputeSpace = items.foldLeft(0L) {
    case (acc, hdd) if !hdd.isEmpty => acc + (Option(api.Driver.driverFor(hdd)) match {
      case Some(driver) => driver.createEnvironment(hdd, this) match {
        case fs: FileSystem =>
          val nbt = driver.dataTag(hdd)
          fs.loadData(nbt)
          fs.fileSystem.close()
          fs.fileSystem.list("/").foreach(fs.fileSystem.delete)
          fs.saveData(nbt)
          fs.fileSystem.spaceTotal.toInt
        case _ => 0L // Ignore.
      }
      case _ => 0L
    })
    case (acc, ItemStack.EMPTY) => acc
  }

  // ----------------------------------------------------------------------- //

  private final val FileSystemTag = Settings.namespace + "fs"
  private final val PresenceTag = Settings.namespace + "presence"
  private final val LabelTag = Settings.namespace + "label"

  override def loadForServer(nbt: CompoundNBT) {
    super.loadForServer(nbt)
    if (nbt.contains(FileSystemTag)) {
      val tag = nbt.getCompound(FileSystemTag)
      tryCreateRaid(tag.getCompound(NodeData.NodeTag).getString(NodeData.AddressTag))
      filesystem.foreach(fs => fs.loadData(tag))
    }
    label.loadData(nbt)
  }

  override def saveForServer(nbt: CompoundNBT) {
    super.saveForServer(nbt)
    filesystem.foreach(fs => nbt.setNewCompoundTag(FileSystemTag, fs.saveData))
    label.saveData(nbt)
  }

  @OnlyIn(Dist.CLIENT) override
  def loadForClient(nbt: CompoundNBT) {
    super.loadForClient(nbt)
    nbt.getByteArray(PresenceTag).
      map(_ != 0).
      copyToArray(presence)
    label.setLabel(nbt.getString(LabelTag))
  }

  override def saveForClient(nbt: CompoundNBT) {
    super.saveForClient(nbt)
    nbt.put(PresenceTag, items.map(!_.isEmpty))
    if (label.getLabel != null)
      nbt.putString(LabelTag, label.getLabel)
  }

  // ----------------------------------------------------------------------- //

  class RaidLabel extends Label {
    var label = "raid"

    override def getLabel = label

    override def setLabel(value: String) = label = Option(value).map(_.take(16)).orNull

    override def loadData(nbt: CompoundNBT) {
      if (nbt.contains(Settings.namespace + "label")) {
        label = nbt.getString(Settings.namespace + "label")
      }
    }

    override def saveData(nbt: CompoundNBT) {
      nbt.putString(Settings.namespace + "label", label)
    }
  }

}
