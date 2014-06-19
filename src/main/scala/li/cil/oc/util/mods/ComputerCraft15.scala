package li.cil.oc.util.mods

import dan200.computer.api.{IMedia, IMount, IWritableMount}
import li.cil.oc
import li.cil.oc.server.fs.{CC15FileSystem, CC15WritableFileSystem}
import net.minecraft.item.ItemStack
import net.minecraft.world.World

object ComputerCraft15 {
  def isDisk(stack: ItemStack) = stack.getItem.isInstanceOf[IMedia]

  def createDiskMount(stack: ItemStack, world: World) =
    if (isDisk(stack)) oc.api.FileSystem.fromComputerCraft(stack.getItem.asInstanceOf[IMedia].createDataMount(stack, world)) else null

  def createFileSystem(mount: AnyRef) = Option(mount) collect {
    case rw: IWritableMount => new CC15WritableFileSystem(rw)
    case ro: IMount => new CC15FileSystem(ro)
  }
}
