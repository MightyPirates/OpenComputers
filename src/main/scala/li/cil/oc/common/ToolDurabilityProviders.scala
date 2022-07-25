package li.cil.oc.common

import java.lang.reflect.Method

import net.minecraft.item.ItemStack

import scala.collection.mutable

object ToolDurabilityProviders {
  private val providers = mutable.ArrayBuffer.empty[Method]

  def add(provider: Method): Unit = providers += provider

  def getDurability(stack: ItemStack): Option[Double] = {
    for (provider <- providers) {
      val durability = IMC.tryInvokeStatic(provider, stack)(Double.NaN)
      if (!durability.isNaN) return Option(durability)
    }
    // Fall back to vanilla damage values.
    if (stack.getItem.canBeDepleted) Option(1.0 - stack.getDamageValue.toDouble / stack.getMaxDamage.toDouble)
    else None
  }
}
