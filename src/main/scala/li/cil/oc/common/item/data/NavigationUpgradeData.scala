package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemMap
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

class NavigationUpgradeData extends ItemData(Constants.ItemName.NavigationUpgrade) {
  def this(stack: ItemStack) {
    this()
    load(stack)
  }

  var map = new ItemStack(net.minecraft.init.Items.FILLED_MAP)

  def mapData(world: World) = try map.getItem.asInstanceOf[ItemMap].getMapData(map, world) catch {
    case _: Throwable => throw new Exception("invalid map")
  }

  def getSize(world: World) = {
    val info = mapData(world)
    128 * (1 << info.scale)
  }

  private final val DataTag = Constants.namespace + "data"
  private final val MapTag = Constants.namespace + "map"

  override def load(stack: ItemStack) {
    if (stack.hasTagCompound) {
      load(stack.getTagCompound.getCompoundTag(DataTag))
    }
  }

  override def save(stack: ItemStack) {
    if (!stack.hasTagCompound) {
      stack.setTagCompound(new NBTTagCompound())
    }
    save(stack.getCompoundTag(DataTag))
  }

  override def load(nbt: NBTTagCompound) {
    if (nbt.hasKey(MapTag)) {
      map = new ItemStack(nbt.getCompoundTag(MapTag))
    }
  }

  override def save(nbt: NBTTagCompound) {
    if (map != null) {
      nbt.setNewCompoundTag(MapTag, map.writeToNBT)
    }
  }
}
