package li.cil.oc.common.util

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import scala.collection.mutable

/**
 * This singleton is responsible for caching actual item component instances,
 * that is the "component" object belonging to an ItemStack, based on its NBT
 * data.
 */
object ItemComponentCache {
  private val caches = mutable.Map.empty[Int, Cache[_]]

  def get[T](item: ItemStack) = caches.get(item.itemID) match {
    case None => None
    case Some(cache) => cache.asInstanceOf[Cache[T]].getComponent(item)
  }

  def register[T](id: Int, constructor: (NBTTagCompound) => T): Unit =
    caches += id -> new Cache[T](id, constructor)

  private class Cache[T](val id: Int, val constructor: (NBTTagCompound) => T) {
    private val instances = mutable.WeakHashMap.empty[NBTTagCompound, T]

    def getComponent(item: ItemStack): Option[T] =
      if (item.itemID == id) {
        val nbt = item.getTagCompound match {
          case null => new NBTTagCompound
          case tag => tag
        }
        instances.get(nbt).orElse {
          val component = constructor(nbt)
          instances += nbt -> component
          Some(component)
        }
      }
      else throw new IllegalArgumentException("Invalid item type.")
  }

}