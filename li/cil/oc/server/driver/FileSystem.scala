package li.cil.oc.server.driver

import li.cil.oc
import li.cil.oc.api.driver.{Item, Slot}
import li.cil.oc.common.item.{Disk, HardDiskDrive}
import li.cil.oc.{Config, Items}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

object FileSystem extends Item {
  override def api = Option(getClass.getResourceAsStream(Config.driverPath + "filesystem.lua"))

  override def worksWith(item: ItemStack) = WorksWith(Items.hdd1, Items.hdd2, Items.hdd3, Items.disk)(item)

  override def slot(item: ItemStack) = Items.multi.subItem(item) match {
    case Some(hdd: HardDiskDrive) => Slot.HardDiskDrive
    case Some(disk: Disk) => Slot.Disk
    case _ => throw new IllegalArgumentException()
  }

  override def node(item: ItemStack) = Items.multi.subItem(item) match {
    case Some(hdd: HardDiskDrive) => createNode(item, hdd.megaBytes * 1024 * 1024)
    case Some(disk: Disk) => createNode(item, 512 * 1024)
    case _ => None
  }

  private def createNode(item: ItemStack, capacity: Int) = {
    // We have a bit of a chicken-egg problem here, because we want to use the
    // node's address as the folder name... so we generate the address here,
    // if necessary. No one will know, right? Right!?
    val address = addressFromTag(nbt(item))
    oc.api.FileSystem.fromSaveDirectory(address, capacity, Config.filesBuffered).
      flatMap(oc.api.FileSystem.asNode) match {
      case Some(node) =>
        node.address = Some(address)
        Some(node)
      case None => None
    }
  }

  private def addressFromTag(tag: NBTTagCompound) =
    if (tag.hasKey("address")) tag.getString("address")
    else java.util.UUID.randomUUID().toString
}