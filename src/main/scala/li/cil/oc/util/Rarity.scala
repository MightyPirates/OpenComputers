package li.cil.oc.util

import net.minecraft.item.{Rarity => _Rarity}

object Rarity {
  import _Rarity._
  private val lookup = Array(_Rarity.COMMON, _Rarity.UNCOMMON, _Rarity.RARE, _Rarity.EPIC)

  def byTier(tier: Int) = lookup(tier max 0 min (lookup.length - 1))
}
