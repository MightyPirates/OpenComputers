package li.cil.oc.server.driver.item

import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.common.Slot
import li.cil.oc.util.ItemUtils
import li.cil.oc.{Settings, api}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

object Tablet extends Item {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("tablet"))

  override def worksWith(stack: ItemStack, host: EnvironmentHost) =
    super.worksWith(stack, host) && isTablet(host)

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = {
    val data = new ItemUtils.TabletData(stack)
    data.items.collect {
      case Some(fs) if FileSystem.worksWith(fs, host) => fs
    }.headOption.map(FileSystem.createEnvironment(_, host)).orNull
  }

  override def slot(stack: ItemStack) = Slot.Tablet

  override def dataTag(stack: ItemStack) = {
    val data = new ItemUtils.TabletData(stack)
    val index = data.items.indexWhere {
      case Some(fs) => FileSystem.worksWith(fs, null) // This is only safe because we know fs doesn't touch the host parameter.
      case _ => false
    }
    if (index >= 0 && stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "items")) {
      stack.getTagCompound.getTagList(Settings.namespace + "items", NBT.TAG_COMPOUND).getCompoundTagAt(index).getCompoundTag("tag")
    }
    else new NBTTagCompound()
  }
}
