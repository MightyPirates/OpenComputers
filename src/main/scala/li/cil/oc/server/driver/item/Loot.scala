package li.cil.oc.server.driver.item

import li.cil.oc
import li.cil.oc.api.driver.Slot
import li.cil.oc.{api, OpenComputers, Settings}
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity

object Loot extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("lootDisk"))

  override def createEnvironment(stack: ItemStack, container: TileEntity) =
    createEnvironment(stack, 0, container)

  override def slot(stack: ItemStack) = Slot.Disk

  override def tier(stack: ItemStack) = 0

  private def createEnvironment(stack: ItemStack, capacity: Int, container: TileEntity) = {
    if (stack.hasTagCompound) {
      val path = "loot/" + stack.getTagCompound.getString(Settings.namespace + "lootPath")
      val label =
        if (dataTag(stack).hasKey(Settings.namespace + "fs.label")) {
          dataTag(stack).getString(Settings.namespace + "fs.label")
        }
        else null
      val fs = oc.api.FileSystem.fromClass(OpenComputers.getClass, Settings.resourceDomain, path)
      oc.api.FileSystem.asManagedEnvironment(fs, label, container)
    }
    else null
  }
}