package li.cil.oc.integration.util

import dan200.computercraft.api.filesystem.IMount
import dan200.computercraft.api.filesystem.IWritableMount
import dan200.computercraft.api.media.IMedia
import li.cil.oc
import li.cil.oc.server.fs.ComputerCraftFileSystem
import li.cil.oc.server.fs.ComputerCraftWritableFileSystem
import net.minecraft.item.ItemStack
import net.minecraft.world.World

object ComputerCraft {
  def isDisk(stack: ItemStack) = stack.getItem.isInstanceOf[IMedia]

  def createDiskMount(stack: ItemStack, world: World) =
    if (isDisk(stack)) oc.api.FileSystem.fromComputerCraft(stack.getItem.asInstanceOf[IMedia].createDataMount(stack, world)) else null

  def createFileSystem(mount: AnyRef) = Option(mount) collect {
    case rw: IWritableMount => new ComputerCraftWritableFileSystem(rw)
    case ro: IMount => new ComputerCraftFileSystem(ro)
  }
}
