package li.cil.oc.common.item.data

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.FilledMapItem
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.world.World

class NavigationUpgradeData extends ItemData(Constants.ItemName.NavigationUpgrade) {
  def this(stack: ItemStack) {
    this()
    loadData(stack)
  }

  var map = new ItemStack(net.minecraft.item.Items.FILLED_MAP)

  def mapData(world: World) = try FilledMapItem.getSavedData(map, world) catch {
    case _: Throwable => throw new Exception("invalid map")
  }

  def getSize(world: World) = {
    val info = mapData(world)
    128 * (1 << info.scale)
  }

  private final val DataTag = Settings.namespace + "data"
  private final val MapTag = Settings.namespace + "map"

  override def loadData(stack: ItemStack) {
    if (stack.hasTag) {
      loadData(stack.getTag.getCompound(DataTag))
    }
  }

  override def saveData(stack: ItemStack) {
    saveData(stack.getOrCreateTagElement(DataTag))
  }

  override def loadData(nbt: CompoundNBT) {
    if (nbt.contains(MapTag)) {
      map = ItemStack.of(nbt.getCompound(MapTag))
    }
  }

  override def saveData(nbt: CompoundNBT) {
    if (map != null) {
      nbt.setNewCompoundTag(MapTag, map.save)
    }
  }
}
