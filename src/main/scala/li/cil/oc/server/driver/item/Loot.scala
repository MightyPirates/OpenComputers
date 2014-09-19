package li.cil.oc.server.driver.item

import java.io

import li.cil.oc.api.driver.Host
import li.cil.oc.common.Slot
import li.cil.oc.server.fs.FileSystem.ItemLabel
import li.cil.oc.{OpenComputers, Settings, api}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.DimensionManager

object Loot extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("lootDisk"), api.Items.get("openOS"))

  override def createEnvironment(stack: ItemStack, host: Host) =
    if (stack.hasTagCompound) {
      val lootPath = "loot/" + stack.getTagCompound.getString(Settings.namespace + "lootPath")
      val savePath = new io.File(DimensionManager.getCurrentSaveRootDirectory, Settings.savePath + lootPath)
      val fs =
        if (savePath.exists && savePath.isDirectory) {
          api.FileSystem.fromSaveDirectory(lootPath, 0, false)
        }
        else {
          api.FileSystem.fromClass(OpenComputers.getClass, Settings.resourceDomain, lootPath)
        }
      val label =
        if (dataTag(stack).hasKey(Settings.namespace + "fs.label")) {
          dataTag(stack).getString(Settings.namespace + "fs.label")
        }
        else null
      api.FileSystem.asManagedEnvironment(fs, new ReadOnlyItemLabel(stack, label), host)
    }
    else null

  override def slot(stack: ItemStack) = Slot.Floppy

  private class ReadOnlyItemLabel(stack: ItemStack, val label: String) extends ItemLabel(stack) {
    def setLabel(value: String) = throw new IllegalArgumentException("label is read only")

    def getLabel = label

    override def load(nbt: NBTTagCompound) {}

    override def save(nbt: NBTTagCompound) {
      if (label != null) {
        nbt.setString(Settings.namespace + "fs.label", label)
      }
    }
  }

}