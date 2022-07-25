package li.cil.oc.integration.opencomputers

import java.io

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import net.minecraft.item.ItemStack
import net.minecraft.world.storage.FolderName
import net.minecraftforge.fml.server.ServerLifecycleHooks

// This is deprecated and kept for compatibility with old saves.
// As of OC 1.5.10, loot disks are generated using normal floppies, and using
// a factory system that allows third-party mods to register loot disks.
object DriverLootDisk extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.Floppy)) &&
    (stack.hasTag && stack.getTag.contains(Settings.namespace + "lootPath"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (!host.world.isClientSide && stack.hasTag && ServerLifecycleHooks.getCurrentServer != null) {
      val lootPath = Settings.savePath + "loot/" + stack.getTag.getString(Settings.namespace + "lootPath")
      val savePath = ServerLifecycleHooks.getCurrentServer.getWorldPath(new FolderName(lootPath)).toFile
      val fs =
        if (savePath.exists && savePath.isDirectory) {
          api.FileSystem.fromSaveDirectory(lootPath, 0, false)
        }
        else {
          api.FileSystem.fromClass(OpenComputers.getClass, Settings.resourceDomain, lootPath)
        }
      val label =
        if (dataTag(stack).contains(Settings.namespace + "fs.label")) {
          dataTag(stack).getString(Settings.namespace + "fs.label")
        }
        else null
      api.FileSystem.asManagedEnvironment(fs, label, host, Settings.resourceDomain + ":floppy_access")
    }
    else null

  override def slot(stack: ItemStack) = Slot.Floppy
}