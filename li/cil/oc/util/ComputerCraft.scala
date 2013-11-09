package li.cil.oc.util

import cpw.mods.fml.common.Loader
import dan200.computer.api.{IWritableMount, IMount, IMedia}
import li.cil.oc
import net.minecraft.item.ItemStack
import net.minecraft.world.World

object ComputerCraft {
  def isDisk(item: ItemStack) = Loader.isModLoaded("ComputerCraft") && item.getItem.isInstanceOf[IMedia]

  def createDiskMount(item: ItemStack, world: World) = if (isDisk(item)) {
    item.getItem.asInstanceOf[IMedia].createDataMount(item, world) match {
      case mount: IWritableMount => oc.api.FileSystem.fromComputerCraft(mount)
      case mount: IMount => oc.api.FileSystem.fromComputerCraft(mount)
      case _ => null
    }
  } else null
}
