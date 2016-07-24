package li.cil.oc.integration.opencomputers

import li.cil.oc
import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Loot
import li.cil.oc.common.Slot
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.FloppyDisk
import li.cil.oc.common.item.HardDiskDrive
import li.cil.oc.common.item.data.DriveData
import li.cil.oc.server.component.Drive
import li.cil.oc.server.fs.FileSystem.ItemLabel
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.DimensionManager

object DriverFileSystem extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.HDDTier1),
    api.Items.get(Constants.ItemName.HDDTier2),
    api.Items.get(Constants.ItemName.HDDTier3),
    api.Items.get(Constants.ItemName.Floppy)) &&
    (!stack.hasTagCompound || !stack.getTagCompound.hasKey(Settings.namespace + "lootPath"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.world != null && host.world.isRemote) null
    else Delegator.subItem(stack) match {
      case Some(hdd: HardDiskDrive) => createEnvironment(stack, hdd.kiloBytes * 1024, hdd.platterCount, host, hdd.tier + 2)
      case Some(disk: FloppyDisk) => createEnvironment(stack, Settings.get.floppySize * 1024, 1, host, 1)
      case _ => null
    }

  override def slot(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(hdd: HardDiskDrive) => Slot.HDD
      case Some(disk: FloppyDisk) => Slot.Floppy
      case _ => throw new IllegalArgumentException()
    }

  override def tier(stack: ItemStack) =
    Delegator.subItem(stack) match {
      case Some(hdd: HardDiskDrive) => hdd.tier
      case _ => 0
    }

  private def createEnvironment(stack: ItemStack, capacity: Int, platterCount: Int, host: EnvironmentHost, speed: Int) = if (DimensionManager.getWorld(0) != null) {
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "lootFactory")) {
      // Loot disk, create file system using factory callback.
      Loot.factories.get(stack.getTagCompound.getString(Settings.namespace + "lootFactory")) match {
        case Some(factory) =>
          val label =
            if (dataTag(stack).hasKey(Settings.namespace + "fs.label"))
              dataTag(stack).getString(Settings.namespace + "fs.label")
            else null
          api.FileSystem.asManagedEnvironment(factory.call(), label, host, Settings.resourceDomain + ":floppy_access")
        case _ => null // Invalid loot disk.
      }
    }
    else {
      // We have a bit of a chicken-egg problem here, because we want to use the
      // node's address as the folder name... so we generate the address here,
      // if necessary. No one will know, right? Right!?
      val address = addressFromTag(dataTag(stack))
      val label = new ReadWriteItemLabel(stack)
      val isFloppy = api.Items.get(stack) == api.Items.get(Constants.ItemName.Floppy)
      val sound = Settings.resourceDomain + ":" + (if (isFloppy) "floppy_access" else "hdd_access")
      val drive = new DriveData(stack)
      val environment = if (drive.isUnmanaged) {
        new Drive(capacity max 0, platterCount, label, Option(host), Option(sound), speed)
      }
      else {
        val fs = oc.api.FileSystem.fromSaveDirectory(address, capacity max 0, Settings.get.bufferChanges)
        oc.api.FileSystem.asManagedEnvironment(fs, label, host, sound, speed)
      }
      if (environment != null && environment.node != null) {
        environment.node.asInstanceOf[oc.server.network.Node].address = address
      }
      environment
    }
  }
  else null

  private def addressFromTag(tag: NBTTagCompound) =
    if (tag.hasKey("node") && tag.getCompoundTag("node").hasKey("address")) {
      tag.getCompoundTag("node").getString("address")
    }
    else java.util.UUID.randomUUID().toString

  private class ReadWriteItemLabel(stack: ItemStack) extends ItemLabel(stack) {
    var label: Option[String] = None

    override def getLabel = label.orNull

    override def setLabel(value: String) {
      label = Option(value).map(_.take(16))
    }

    private final val LabelTag = Settings.namespace + "fs.label"

    override def load(nbt: NBTTagCompound) {
      if (nbt.hasKey(LabelTag)) {
        label = Option(nbt.getString(LabelTag))
      }
    }

    override def save(nbt: NBTTagCompound) {
      label match {
        case Some(value) => nbt.setString(LabelTag, value)
        case _ =>
      }
    }
  }

}