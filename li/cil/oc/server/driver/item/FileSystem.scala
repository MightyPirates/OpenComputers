package li.cil.oc.server.driver.item

import dan200.computer.api.IMedia
import li.cil.oc
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.fs.Label
import li.cil.oc.common.item.{Disk, HardDiskDrive}
import li.cil.oc.common.tileentity.DiskDrive
import li.cil.oc.util.mods.ComputerCraft
import li.cil.oc.{Config, Items}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

object FileSystem extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, Items.hdd1, Items.hdd2, Items.hdd3, Items.disk) || ComputerCraft.isDisk(stack)

  override def createEnvironment(stack: ItemStack, container: AnyRef) =
    if (ComputerCraft.isDisk(stack)) {
      container match {
        case diskDrive: DiskDrive =>
          val address = addressFromTag(nbt(stack))
          val mount = ComputerCraft.createDiskMount(stack, diskDrive.world)
          Option(oc.api.FileSystem.asManagedEnvironment(mount, new ComputerCraftLabel(stack))) match {
            case Some(environment) =>
              environment.node.asInstanceOf[oc.server.network.Node].address = address
              environment
            case _ => null
          }
        case _ => null
      }
    } else Items.multi.subItem(stack) match {
      case Some(hdd: HardDiskDrive) => createEnvironment(stack, hdd.kiloBytes * 1024)
      case Some(disk: Disk) => createEnvironment(stack, 512 * 1024)
      case _ => null
    }

  override def slot(stack: ItemStack) =
    if (ComputerCraft.isDisk(stack)) Slot.Disk
    else Items.multi.subItem(stack) match {
      case Some(hdd: HardDiskDrive) => Slot.HardDiskDrive
      case Some(disk: Disk) => Slot.Disk
      case _ => throw new IllegalArgumentException()
    }

  private def createEnvironment(stack: ItemStack, capacity: Int) = {
    // We have a bit of a chicken-egg problem here, because we want to use the
    // node's address as the folder name... so we generate the address here,
    // if necessary. No one will know, right? Right!?
    val address = addressFromTag(nbt(stack))
    Option(oc.api.FileSystem.asManagedEnvironment(oc.api.FileSystem.
      fromSaveDirectory(address, capacity, Config.bufferChanges), new ItemLabel(stack))) match {
      case Some(environment) =>
        environment.node.asInstanceOf[oc.server.network.Node].address = address
        environment
      case _ => null
    }
  }

  private def addressFromTag(tag: NBTTagCompound) =
    if (tag.hasKey(Config.namespace + "node.address")) tag.getString(Config.namespace + "node.address")
    else java.util.UUID.randomUUID().toString

  private class ComputerCraftLabel(val stack: ItemStack) extends Label {
    val media = stack.getItem.asInstanceOf[IMedia]

    def getLabel = media.getLabel(stack)

    def setLabel(value: String) {
      media.setLabel(stack, value)
    }
  }

  private class ItemLabel(val stack: ItemStack) extends Label {
    def getLabel =
      if (nbt(stack).hasKey(Config.namespace + "fs.label"))
        nbt(stack).getString(Config.namespace + "fs.label")
      else null

    def setLabel(value: String) {
      nbt(stack).setString(Config.namespace + "fs.label",
        if (value.length > 16) value.substring(0, 16) else value)
    }
  }

}