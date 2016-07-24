package li.cil.oc.util

import net.minecraft.item.EnumRarity

object Rarity {
  private val lookup = Array(EnumRarity.COMMON, EnumRarity.UNCOMMON, EnumRarity.RARE, EnumRarity.EPIC)

  def byTier(tier: Int) = lookup(tier max 0 min (lookup.length - 1))
}
