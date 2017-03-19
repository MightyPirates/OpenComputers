package li.cil.oc.integration.opencomputers

import java.io

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.util.Location
import li.cil.oc.common.Slot
import net.minecraft.item.ItemStack
import net.minecraftforge.common.DimensionManager

// This is deprecated and kept for compatibility with old saves.
// As of OC 1.5.10, loot disks are generated using normal floppies, and using
// a factory system that allows third-party mods to register loot disks.
object DriverLootDisk extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.Floppy)) &&
    (stack.hasTagCompound && stack.getTagCompound.hasKey(Constants.namespace + "lootPath"))

  override def createEnvironment(stack: ItemStack, host: Location) =
    if (!host.getWorld.isRemote && stack.hasTagCompound && DimensionManager.getWorld(0) != null) {
      val lootPath = "loot/" + stack.getTagCompound.getString(Constants.namespace + "lootPath")
      val savePath = new io.File(DimensionManager.getCurrentSaveRootDirectory, Settings.savePath + lootPath)
      val fs =
        if (savePath.exists && savePath.isDirectory) {
          api.FileSystem.fromSaveDirectory(lootPath, 0, false)
        }
        else {
          api.FileSystem.fromClass(OpenComputers.getClass, Constants.resourceDomain, lootPath)
        }
      val label =
        if (dataTag(stack).hasKey(Constants.namespace + "fs.label")) {
          dataTag(stack).getString(Constants.namespace + "fs.label")
        }
        else null
      api.FileSystem.asManagedEnvironment(fs, label, host, Constants.resourceDomain + ":floppy_access")
    }
    else null

  override def slot(stack: ItemStack) = Slot.Floppy
}