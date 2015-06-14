package li.cil.oc.common.item.data

import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemMap
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

class NavigationUpgradeData extends ItemData {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var map = new ItemStack(net.minecraft.init.Items.filled_map)

  def mapData(world: World) = try map.getItem.asInstanceOf[ItemMap].getMapData(map, world) catch {
    case _: Throwable => throw new Exception("invalid map")
  }

  override def load(stack: ItemStack) {
    if (stack.hasTagCompound) {
      load(stack.getTagCompound.getCompoundTag(Settings.namespace + "data"))
    }
  }

  override def save(stack: ItemStack) {
    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound())
    }
    save(stack.getCompoundTag(Settings.namespace + "data"))
  }

  override def load(nbt: NBTTagCompound) {
    if (nbt.hasKey(Settings.namespace + "map")) {
      map = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(Settings.namespace + "map"))
    }
  }

  override def save(nbt: NBTTagCompound) {
    if (map != null) {
      nbt.setNewCompoundTag(Settings.namespace + "map", map.writeToNBT)
    }
  }
}
