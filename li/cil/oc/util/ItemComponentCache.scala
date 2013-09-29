package li.cil.oc.util

import com.google.common.collect.MapMaker
import li.cil.oc.Items
import li.cil.oc.server.component.ItemComponent
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import scala.collection.{JavaConversions, mutable}

/**
 * This singleton is responsible for caching actual item component instances,
 * that is the "component" object belonging to an ItemStack, based on its NBT
 * data.
 */
object ItemComponentCache {
  private val caches = mutable.Map.empty[Int, Cache[_]]

  def get[T <: ItemComponent](item: ItemStack) = if (item.itemID == Items.multi.itemID)
    Items.multi.subItem(item) match {
      case None => None
      case Some(subItem) => caches.get(subItem.itemId) match {
        case None => None
        case Some(cache) => cache.asInstanceOf[Cache[T]].get(item)
      }
    } else None

  def register[T](id: Int, constructor: (NBTTagCompound) => T): Unit =
    caches += id -> new Cache[T](id, constructor)

  private class Cache[T](val id: Int, val constructor: (NBTTagCompound) => T) {
    private val instances = JavaConversions.mapAsScalaMap(new MapMaker().weakKeys().makeMap[NBTTagCompound, T]())

    def get(item: ItemStack): Option[T] = {
      val nbt = item.getTagCompound match {
        case null => new NBTTagCompound
        case tag => tag
      }
      item.setTagCompound(nbt)
      instances.get(nbt) orElse {
        val component = constructor(nbt)
        instances += nbt -> component
        Some(component)
      }
    }
  }

}