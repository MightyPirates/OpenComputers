package li.cil.oc.server.driver.item

import li.cil.oc
import li.cil.oc.api.driver.{Container, Slot}
import li.cil.oc.common.item.{FloppyDisk, HardDiskDrive}
import li.cil.oc.server.fs.FileSystem.ItemLabel
import li.cil.oc.{Items, Settings, api}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

object FileSystem extends Item {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("hdd1"), api.Items.get("hdd2"), api.Items.get("hdd3"), api.Items.get("floppy"))

  override def createEnvironment(stack: ItemStack, container: Container) =
    Items.multi.subItem(stack) match {
      case Some(hdd: HardDiskDrive) => createEnvironment(stack, hdd.kiloBytes * 1024, container)
      case Some(disk: FloppyDisk) => createEnvironment(stack, Settings.get.floppySize * 1024, container)
      case _ => null
    }

  override def slot(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(hdd: HardDiskDrive) => Slot.HardDiskDrive
      case Some(disk: FloppyDisk) => Slot.Disk
      case _ => throw new IllegalArgumentException()
    }

  override def tier(stack: ItemStack) =
    Items.multi.subItem(stack) match {
      case Some(hdd: HardDiskDrive) => hdd.tier
      case _ => 0
    }

  private def createEnvironment(stack: ItemStack, capacity: Int, container: Container) = {
    // We have a bit of a chicken-egg problem here, because we want to use the
    // node's address as the folder name... so we generate the address here,
    // if necessary. No one will know, right? Right!?
    val address = addressFromTag(dataTag(stack))
    val fs = oc.api.FileSystem.fromSaveDirectory(address, capacity, Settings.get.bufferChanges)
    val environment = oc.api.FileSystem.asManagedEnvironment(fs, new ReadWriteItemLabel(stack), container)
    if (environment != null && environment.node != null) {
      environment.node.asInstanceOf[oc.server.network.Node].address = address
    }
    environment
  }

  private def addressFromTag(tag: NBTTagCompound) =
    if (tag.hasKey("node") && tag.getCompoundTag("node").hasKey("address")) {
      tag.getCompoundTag("node").getString("address")
    }
    else java.util.UUID.randomUUID().toString

  private class ReadWriteItemLabel(stack: ItemStack) extends ItemLabel(stack) {
    var label: Option[String] = None

    override def getLabel = label.orNull

    override def setLabel(value: String) {
      label = Option(if (value != null && value.length > 16) value.substring(0, 16) else value)
    }

    override def load(nbt: NBTTagCompound) {
      if (nbt.hasKey(Settings.namespace + "fs.label")) {
        label = Option(nbt.getString(Settings.namespace + "fs.label"))
      }
    }

    override def save(nbt: NBTTagCompound) {
      label match {
        case Some(value) => nbt.setString(Settings.namespace + "fs.label", value)
        case _ =>
      }
    }
  }

}