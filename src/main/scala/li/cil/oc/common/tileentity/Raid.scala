package li.cil.oc.common.tileentity

import java.util.UUID

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.fs.Label
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.Slot
import li.cil.oc.server.component.FileSystem
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class Raid extends traits.Environment with traits.Inventory with traits.Rotatable with Analyzable {
  val node = api.Network.newNode(this, Visibility.None).create()

  var filesystem: Option[FileSystem] = None

  val label = new RaidLabel()

  // Used on client side to check whether to render disk activity indicators.
  var lastAccess = 0L

  // For client side rendering.
  val presence = Array.fill(getSizeInventory)(false)

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = Array(filesystem.map(_.node).orNull)

  override def canUpdate = false

  // ----------------------------------------------------------------------- //

  override def getSizeInventory = 3

  override def getInventoryStackLimit = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = Option(Driver.driverFor(stack, getClass)) match {
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

  override def markDirty() {
    super.markDirty()
    // Makes the implementation of the comparator output easier.
    items.map(_.isDefined).copyToArray(presence)
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (isServer) this.synchronized {
      ServerPacketSender.sendRaidChange(this)
      filesystem.foreach(fs => {
        fs.fileSystem.close()
        fs.fileSystem.list("/").foreach(fs.fileSystem.delete)
        fs.save(new NBTTagCompound()) // Flush buffered fs.
        fs.node.remove()
        filesystem = None
      })
    }
  }

  def tryCreateRaid(id: String) {
    if (items.count(_.isDefined) == items.length && filesystem.fold(true)(fs => fs.node == null || fs.node.address != id)) {
      filesystem.foreach(fs => if (fs.node != null) fs.node.remove())
      val fs = api.FileSystem.asManagedEnvironment(
        api.FileSystem.fromSaveDirectory(id, wipeDisksAndComputeSpace, Settings.get.bufferChanges),
        label, this, Settings.resourceDomain + ":hdd_access", 6).
        asInstanceOf[FileSystem]
      val nbtToSetAddress = new NBTTagCompound()
      nbtToSetAddress.setString("address", id)
      fs.node.load(nbtToSetAddress)
      fs.node.setVisibility(Visibility.Network)
      // Ensure we're in a network before connecting the raid fs.
      api.Network.joinNewNetwork(node)
      node.connect(fs.node)
      filesystem = Option(fs)
    }
  }

  private def wipeDisksAndComputeSpace = items.foldLeft(0L) {
    case (acc, Some(hdd)) => acc + (Option(api.Driver.driverFor(hdd)) match {
      case Some(driver) => driver.createEnvironment(hdd, this) match {
        case fs: FileSystem =>
          val nbt = driver.dataTag(hdd)
          fs.load(nbt)
          fs.fileSystem.close()
          fs.fileSystem.list("/").foreach(fs.fileSystem.delete)
          fs.save(nbt)
          fs.fileSystem.spaceTotal.toInt
        case _ => 0L // Ignore.
      }
      case _ => 0L
    })
    case (acc, None) => acc
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    super.readFromNBTForServer(nbt)
    if (nbt.hasKey(Settings.namespace + "fs")) {
      val tag = nbt.getCompoundTag(Settings.namespace + "fs")
      tryCreateRaid(tag.getCompoundTag("node").getString("address"))
      filesystem.foreach(fs => fs.load(tag))
    }
    label.load(nbt)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    super.writeToNBTForServer(nbt)
    filesystem.foreach(fs => nbt.setNewCompoundTag(Settings.namespace + "fs", fs.save))
    label.save(nbt)
  }

  @SideOnly(Side.CLIENT) override
  def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    nbt.getByteArray("presence").
      map(_ != 0).
      copyToArray(presence)
    label.setLabel(nbt.getString("label"))
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setTag("presence", items.map(_.isDefined))
    if (label.getLabel != null)
      nbt.setString("label", label.getLabel)
  }

  // ----------------------------------------------------------------------- //

  class RaidLabel extends Label {
    var label = "raid"

    override def getLabel = label

    override def setLabel(value: String) = label = Option(value).map(_.take(16)).orNull

    override def load(nbt: NBTTagCompound) {
      if (nbt.hasKey(Settings.namespace + "label")) {
        label = nbt.getString(Settings.namespace + "label")
      }
    }

    override def save(nbt: NBTTagCompound) {
      nbt.setString(Settings.namespace + "label", label)
    }
  }
}
