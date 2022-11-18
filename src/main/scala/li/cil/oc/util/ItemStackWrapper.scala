package li.cil.oc.util

import java.util.Objects

import net.minecraft.item.Item
import net.minecraft.item.ItemStack

import scala.language.implicitConversions

class ItemStackWrapper(val inner: ItemStack) extends Ordered[ItemStackWrapper] {
  def id = if (inner.getItem != null) Item.getId(inner.getItem) else 0

  def damage = if (inner.getItem != null) inner.getDamageValue else 0

  override def compare(that: ItemStackWrapper) = {
    if (this.id == that.id) this.damage - that.damage
    else this.id - that.id
  }

  override def hashCode() = Objects.hash(Int.box(id), Int.box(damage))

  override def equals(obj: scala.Any) = obj match {
    case that: ItemStackWrapper => compare(that) == 0
    case _ => false
  }

  override def clone() = new ItemStackWrapper(inner)

  override def toString = inner.toString
}
