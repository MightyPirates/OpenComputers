package li.cil.oc.server.driver.item

import java.io

import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.{OpenComputers, Settings, api}
import net.minecraft.item.ItemStack
import net.minecraftforge.common.DimensionManager

object Loot extends Item {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("lootDisk"), api.Items.get("openOS"))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
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
      api.FileSystem.asManagedEnvironment(fs, label, host, "floppy_access")
    }
    else null

  override def slot(stack: ItemStack) = Slot.Floppy
}