package li.cil.oc.server.driver.item

import li.cil.oc.api.driver.{Container, Slot}
import li.cil.oc.util.ItemUtils
import li.cil.oc.{Settings, api}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

object Tablet extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack, api.Items.get("tablet"))

  override def createEnvironment(stack: ItemStack, container: Container) = {
    val data = new ItemUtils.TabletData(stack)
    data.items.collect {
      case Some(fs) if FileSystem.worksWith(fs) => fs
    }.headOption.map(FileSystem.createEnvironment(_, container)).orNull
  }

  override def slot(stack: ItemStack) = Slot.Disk

  override def dataTag(stack: ItemStack) = {
    val data = new ItemUtils.TabletData(stack)
    val index = data.items.indexWhere {
      case Some(fs) => FileSystem.worksWith(fs)
      case _ => false
    }
    if (index >= 0 && stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "items")) {
      stack.getTagCompound.getTagList(Settings.namespace + "items").tagAt(index).asInstanceOf[NBTTagCompound].getCompoundTag("tag")
    }
    else new NBTTagCompound()
  }
}
