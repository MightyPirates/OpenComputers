package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.item.ItemStack

class HardDiskDrive(val parent: Delegator, val tier: Int) extends traits.Delegate with traits.ItemTier with traits.FileSystemLike {
  override val unlocalizedName: String = super.unlocalizedName + tier
  val kiloBytes: Int = Settings.get.hddSizes(tier)
  val platterCount: Int = Settings.get.hddPlatterCounts(tier)

  override def displayName(stack: ItemStack): Some[String] = {
    val localizedName = parent.internalGetItemStackDisplayName(stack)
    Some(if (kiloBytes >= 1024) {
      localizedName + s" (${kiloBytes / 1024}MB)"
    }
    else {
      localizedName + s" (${kiloBytes}KB)"
    })
  }
}