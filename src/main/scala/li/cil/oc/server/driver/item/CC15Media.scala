package li.cil.oc.server.driver.item

import dan200.computer.api.IMedia
import li.cil.oc
import li.cil.oc.api.driver.{Container, Slot}
import li.cil.oc.api.fs.Label
import li.cil.oc.util.mods.{ComputerCraft15, Mods}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

object CC15Media extends Item {
  override def slot(stack: ItemStack) = Slot.Disk

  override def createEnvironment(stack: ItemStack, container: Container) =
    if (Mods.ComputerCraft15.isAvailable && ComputerCraft15.isDisk(stack) && container != null) {
      val address = addressFromTag(dataTag(stack))
      val mount = ComputerCraft15.createDiskMount(stack, container.world)
      Option(oc.api.FileSystem.asManagedEnvironment(mount, new ComputerCraftLabel(stack), container)) match {
        case Some(environment) =>
          environment.node.asInstanceOf[oc.server.network.Node].address = address
          environment
        case _ => null
      }
    }
    else null

  override def worksWith(stack: ItemStack) = Mods.ComputerCraft15.isAvailable && ComputerCraft15.isDisk(stack)

  private def addressFromTag(tag: NBTTagCompound) =
    if (tag.hasKey("node") && tag.getCompoundTag("node").hasKey("address")) {
      tag.getCompoundTag("node").getString("address")
    }
    else java.util.UUID.randomUUID().toString

  class ComputerCraftLabel(val stack: ItemStack) extends Label {
    val media = stack.getItem.asInstanceOf[IMedia]

    override def getLabel = media.getLabel(stack)

    override def setLabel(value: String) {
      media.setLabel(stack, value)
    }

    override def load(nbt: NBTTagCompound) {}

    override def save(nbt: NBTTagCompound) {}
  }

}
