package li.cil.oc.integration.opencomputers

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network.Component
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.Slot
import li.cil.oc.common.item.Tablet
import li.cil.oc.common.item.data.TabletData
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT

object DriverTablet extends Item {
  override def worksWith(stack: ItemStack) = isOneOf(stack,
    api.Items.get(Constants.ItemName.Tablet))

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) =
    if (host.getWorld != null && host.getWorld.isRemote) null
    else {
      Tablet.Server.cache.invalidate(Tablet.getId(stack))
      val data = new TabletData(stack)
      data.items.collect {
        case Some(fs) if DriverFileSystem.worksWith(fs) => fs
      }.headOption.map(DriverFileSystem.createEnvironment(_, host)) match {
        case Some(environment) => environment.getNode match {
          case component: Component =>
            component.setVisibility(Visibility.NETWORK)
            environment.save(dataTag(stack))
            environment
          case _ => null
        }
        case _ => null
      }
    }

  override def slot(stack: ItemStack) = Slot.Tablet

  override def dataTag(stack: ItemStack) = {
    val data = new TabletData(stack)
    val index = data.items.indexWhere {
      case Some(fs) => DriverFileSystem.worksWith(fs)
      case _ => false
    }
    if (index >= 0 && stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "items")) {
      val baseTag = stack.getTagCompound.getTagList(Settings.namespace + "items", NBT.TAG_COMPOUND).getCompoundTagAt(index)
      if (!baseTag.hasKey("item")) {
        baseTag.setTag("item", new NBTTagCompound())
      }
      val itemTag = baseTag.getCompoundTag("item")
      if (!itemTag.hasKey("tag")) {
        itemTag.setTag("tag", new NBTTagCompound())
      }
      val stackTag = itemTag.getCompoundTag("tag")
      if (!stackTag.hasKey(Settings.namespace + "data")) {
        stackTag.setTag(Settings.namespace + "data", new NBTTagCompound())
      }
      stackTag.getCompoundTag(Settings.namespace + "data")
    }
    else new NBTTagCompound()
  }
}
