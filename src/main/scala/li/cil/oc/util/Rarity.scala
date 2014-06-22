package li.cil.oc.util

import net.minecraft.item.EnumRarity

object Rarity {
  private val lookup = Array(EnumRarity.common, EnumRarity.uncommon, EnumRarity.rare, EnumRarity.epic)

  def byTier(tier: Int) = lookup(tier max 0 min (lookup.length - 1))
}
