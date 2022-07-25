package li.cil.oc.integration.computercraft

import dan200.computercraft.api.filesystem.IMount
import dan200.computercraft.api.filesystem.IWritableMount
import dan200.computercraft.api.media.IMedia
import li.cil.oc
import li.cil.oc.Settings
import li.cil.oc.api.fs.FileSystem
import li.cil.oc.api.fs.Label
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.integration.opencomputers.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT

object DriverComputerCraftMedia extends Item {
  override def worksWith(stack: ItemStack) = stack.getItem.isInstanceOf[IMedia]

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = if (!host.world.isClientSide) {
    val address = addressFromTag(dataTag(stack))
    val mount = fromComputerCraft(stack.getItem.asInstanceOf[IMedia].createDataMount(stack, host.world))
    Option(oc.api.FileSystem.asManagedEnvironment(mount, new ComputerCraftLabel(stack), host, Settings.resourceDomain + ":floppy_access")) match {
      case Some(environment) =>
        environment.node.asInstanceOf[oc.server.network.Node].address = address
        environment
      case _ => null
    }
  } else null

  def fromComputerCraft(mount: AnyRef): FileSystem = DriverComputerCraftMedia.createFileSystem(mount).orNull

  override def slot(stack: ItemStack) = Slot.Floppy

  def createFileSystem(mount: AnyRef) = Option(mount) collect {
    case rw: IWritableMount => new ComputerCraftWritableFileSystem(rw)
    case ro: IMount => new ComputerCraftFileSystem(ro)
  }

  private def addressFromTag(tag: CompoundNBT) =
    if (tag.contains("node") && tag.getCompound("node").contains("address")) {
      tag.getCompound("node").getString("address")
    }
    else java.util.UUID.randomUUID().toString

  class ComputerCraftLabel(val stack: ItemStack) extends Label {
    val media = stack.getItem.asInstanceOf[IMedia]

    override def getLabel = media.getLabel(stack)

    override def setLabel(value: String) {
      media.setLabel(stack, value)
    }

    override def loadData(nbt: CompoundNBT) {}

    override def saveData(nbt: CompoundNBT) {}
  }

}
