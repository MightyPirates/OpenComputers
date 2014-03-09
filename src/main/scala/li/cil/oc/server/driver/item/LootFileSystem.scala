package li.cil.oc.server.driver.item

import dan200.computer.api.IMedia
import li.cil.oc
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.fs.Label
import li.cil.oc.common.item.{FloppyDisk, HardDiskDrive}
import li.cil.oc.util.mods.ComputerCraft
import li.cil.oc.{OpenComputers, Settings, Items}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

object LootFileSystem extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, Items.lootDisk)

  override def createEnvironment(stack: ItemStack, container: TileEntity) = {
    createEnvironment(stack, Settings.get.floppySize * 1024, container)
  }

  override def slot(stack: ItemStack) =
    Slot.Disk


  override def tier(stack: ItemStack) = 0

  private def createEnvironment(stack: ItemStack, capacity: Int, container: TileEntity) = {
    // We have a bit of a chicken-egg problem here, because we want to use the
    // node's address as the folder name... so we generate the address here,
    // if necessary. No one will know, right? Right!?
    val address = addressFromTag(dataTag(stack))
    val fs = oc.api.FileSystem.fromClass(OpenComputers.getClass, Settings.resourceDomain, "loot/" + dataTag(stack).getString(Settings.namespace + "lootpath"))

    val environment = oc.api.FileSystem.asManagedEnvironment(fs, new ItemLabel(stack), container)
    if (environment != null) {
      environment.node.asInstanceOf[oc.server.network.Node].address = address
    }
    environment
  }

  private def addressFromTag(tag: NBTTagCompound) =
    if (tag.hasKey("node") && tag.getCompoundTag("node").hasKey("address")) {
      tag.getCompoundTag("node").getString("address")
    }
    else java.util.UUID.randomUUID().toString


  private class ItemLabel(val stack: ItemStack) extends Label {
    var label: Option[String] = None

    override def getLabel = label.orNull

    override def setLabel(value: String) {
      label = Option(if (value != null && value.length > 16) value.substring(0, 16) else value)
    }

    override def load(nbt: NBTTagCompound) {
      if (dataTag(stack).hasKey(Settings.namespace + "fs.label")) {
        label = Option(dataTag(stack).getString(Settings.namespace + "fs.label"))
      }
    }

    override def save(nbt: NBTTagCompound) {
      label match {
        case Some(value) => dataTag(stack).setString(Settings.namespace + "fs.label", value)
        case _ =>
      }
    }
  }

}