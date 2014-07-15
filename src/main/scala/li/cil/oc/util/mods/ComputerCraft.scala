package li.cil.oc.util.mods

import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.filesystem.{IMount, IWritableMount}
import dan200.computercraft.api.lua.ILuaContext
import dan200.computercraft.api.media.IMedia
import dan200.computercraft.api.peripheral.{IComputerAccess, IPeripheral, IPeripheralProvider}
import li.cil.oc
import li.cil.oc.common.tileentity.Switch
import li.cil.oc.server.fs.{ComputerCraftFileSystem, ComputerCraftWritableFileSystem}
import net.minecraft.item.ItemStack
import net.minecraft.world.World

import scala.collection.mutable

object ComputerCraft {
  def init() {
    ComputerCraftAPI.registerPeripheralProvider(new IPeripheralProvider {
      override def getPeripheral(world: World, x: Int, y: Int, z: Int, side: Int) = world.getTileEntity(x, y, z) match {
        case switch: Switch => new SwitchPeripheral(switch)
        case _ => null
      }
    })
  }

  def isDisk(stack: ItemStack) = stack.getItem.isInstanceOf[IMedia]

  def createDiskMount(stack: ItemStack, world: World) =
    if (isDisk(stack)) oc.api.FileSystem.fromComputerCraft(stack.getItem.asInstanceOf[IMedia].createDataMount(stack, world)) else null

  def createFileSystem(mount: AnyRef) = Option(mount) collect {
    case rw: IWritableMount => new ComputerCraftWritableFileSystem(rw)
    case ro: IMount => new ComputerCraftFileSystem(ro)
  }

  class SwitchPeripheral(val switch: Switch) extends IPeripheral {
    override def getType = switch.getType

    override def attach(computer: IComputerAccess) {
      switch.computers += computer
      switch.openPorts += computer -> mutable.Set.empty
    }

    override def detach(computer: IComputerAccess) {
      switch.computers -= computer
      switch.openPorts -= computer
    }

    override def getMethodNames = switch.getMethodNames

    override def callMethod(computer: IComputerAccess, context: ILuaContext, method: Int, arguments: Array[AnyRef]) =
      switch.callMethod(computer, context, method, arguments)

    override def equals(other: IPeripheral) = other match {
      case peripheral: SwitchPeripheral => peripheral.switch == switch
      case _ => false
    }
  }

}
