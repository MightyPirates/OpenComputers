package li.cil.oc.util.mods

import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.filesystem.{IMount, IWritableMount}
import dan200.computercraft.api.media.IMedia
import dan200.computercraft.api.peripheral.{IComputerAccess, IPeripheral, IPeripheralProvider}
import li.cil.oc
import li.cil.oc.common.tileentity.{ComputerWrapper, Router}
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import dan200.computercraft.api.lua.ILuaContext
import scala.collection.mutable

object ComputerCraft16 {
  def init() {
    ComputerCraftAPI.registerPeripheralProvider(new IPeripheralProvider {
      override def getPeripheral(world: World, x: Int, y: Int, z: Int, side: Int) = world.getBlockTileEntity(x, y, z) match {
        case router: Router => new RouterPeripheral(router)
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
  } else null

  class RouterPeripheral(val router: Router) extends IPeripheral {
    override def getType = router.getType

    override def attach(computer: IComputerAccess) {
      router.computers += computer -> new ComputerWrapper {
        override def id = computer.getID

        override def attachmentName = computer.getAttachmentName

        override def queueEvent(name: String, args: Array[AnyRef]) = computer.queueEvent(name, args)
      }
      router.openPorts += computer -> mutable.Set.empty
    }

    override def detach(computer: IComputerAccess) {
      router.computers -= computer
      router.openPorts -= computer
    }

    override def getMethodNames = router.getMethodNames

    override def callMethod(computer: IComputerAccess, context: ILuaContext, method: Int, arguments: Array[AnyRef]) =
      router.callMethod(computer, computer.getID, computer.getAttachmentName, method, arguments)

    override def equals(other: IPeripheral) = other match {
      case rp: RouterPeripheral => rp.router == router
      case _ => false
    }
  }

}
