package li.cil.oc.integration.util

import li.cil.oc.common.item.traits.SimpleItem
import net.minecraft.block.Block
import net.minecraft.item.ItemStack

import scala.collection.mutable

object ItemBlacklist {
  // Lazily evaluated stacks to avoid creating stacks with unregistered items/blocks.
  val hiddenItems = mutable.Set.empty[() => ItemStack]

  // List of consumers for item stacks (blacklisting for NEI and JEI).
  val consumers = mutable.Set.empty[ItemStack => Unit]

  def hide(block: Block): Unit = hiddenItems += (() => new ItemStack(block))

  def hide(item: SimpleItem): Unit = hiddenItems += (() => item.createItemStack())

  def apply(): Unit = {
    for (consumer <- consumers) {
      for (stack <- hiddenItems) {
        consumer(stack())
      }
    }
  }
}
