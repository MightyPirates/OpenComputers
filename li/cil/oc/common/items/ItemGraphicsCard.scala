package li.cil.oc.common.items

import li.cil.oc.Config
import li.cil.oc.CreativeTab
import net.minecraft.item.Item
import net.minecraft.world.World
import scala.collection.mutable.WeakHashMap
import net.minecraft.nbt.NBTTagCompound
import li.cil.oc.server.components.Disk
import net.minecraft.item.ItemStack
import li.cil.oc.server.components.GraphicsCard

object ItemGraphicsCard {
  private val instances = WeakHashMap.empty[NBTTagCompound, GraphicsCard]

  def getComponent(item: ItemStack): Option[GraphicsCard] =
    if (item.itemID == Config.itemGPUId) {
      val tag = item.getTagCompound match {
        case null => new NBTTagCompound
        case tag => tag
      }
      instances.get(tag).orElse {
        val component = new GraphicsCard(tag)
        instances += tag -> component
        Some(component)
      }
    }
    else throw new IllegalArgumentException("Invalid item type.")
}

class ItemGraphicsCard extends Item(Config.itemGPUId) {
  setMaxStackSize(1)
  setHasSubtypes(true)
  setUnlocalizedName("oc.gpu")
  setCreativeTab(CreativeTab)

  override def shouldPassSneakingClickToBlock(world: World, x: Int, y: Int, z: Int) = true
}