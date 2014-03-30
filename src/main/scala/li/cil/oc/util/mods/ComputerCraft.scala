package li.cil.oc.util.mods

import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.filesystem.{IMount, IWritableMount}
import dan200.computercraft.api.media.IMedia
import dan200.computercraft.api.peripheral.IPeripheralProvider
import li.cil.oc
import li.cil.oc.common.tileentity.Router
import net.minecraft.item.ItemStack
import net.minecraft.world.World

object ComputerCraft {
  def init() {
    ComputerCraftAPI.registerPeripheralProvider(new IPeripheralProvider {
      override def getPeripheral(world: World, x: Int, y: Int, z: Int, side: Int) = world.getTileEntity(x, y, z) match {
        case router: Router => router
        case _ => null
      }
    })
  }

  def isDisk(stack: ItemStack) = stack.getItem.isInstanceOf[IMedia]

  def createDiskMount(stack: ItemStack, world: World) = if (isDisk(stack)) {
    stack.getItem.asInstanceOf[IMedia].createDataMount(stack, world) match {
      case mount: IWritableMount => oc.api.FileSystem.fromComputerCraft(mount)
      case mount: IMount => oc.api.FileSystem.fromComputerCraft(mount)
      case _ => null
    }
  }
  else null
}
