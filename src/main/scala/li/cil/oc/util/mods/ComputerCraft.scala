package li.cil.oc.util.mods

import cpw.mods.fml.common.Loader
import dan200.computer.api.{IWritableMount, IMount, IMedia}
import li.cil.oc
import net.minecraft.item.ItemStack
import net.minecraft.world.World

object ComputerCraft {
  def isDisk(stack: ItemStack) = Loader.isModLoaded("ComputerCraft") && stack.getItem.isInstanceOf[IMedia]

  def createDiskMount(stack: ItemStack, world: World) = if (isDisk(stack)) {
    stack.getItem.asInstanceOf[IMedia].createDataMount(stack, world) match {
      case mount: IWritableMount => oc.api.FileSystem.fromComputerCraft(mount)
      case mount: IMount => oc.api.FileSystem.fromComputerCraft(mount)
      case _ => null
    }
  } else null
}
