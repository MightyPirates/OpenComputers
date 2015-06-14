package li.cil.oc.common.item

import java.util

import li.cil.oc.Settings
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class HardDiskDrive(val parent: Delegator, val tier: Int) extends traits.Delegate with traits.ItemTier {
  override val unlocalizedName = super.unlocalizedName + tier
  val kiloBytes = Settings.get.hddSizes(tier)

  override protected def tooltipName = None

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) = {
    if (stack.hasTagCompound) {
      val nbt = stack.getTagCompound
      if (nbt.hasKey(Settings.namespace + "data")) {
        val data = nbt.getCompoundTag(Settings.namespace + "data")
        if (data.hasKey(Settings.namespace + "fs.label")) {
          tooltip.add(data.getString(Settings.namespace + "fs.label"))
        }
        if (advanced && data.hasKey("fs")) {
          val fsNbt = data.getCompoundTag("fs")
          if (fsNbt.hasKey("capacity.used")) {
            val used = fsNbt.getLong("capacity.used")
            tooltip.add(s"Disk usage: $used/${kiloBytes * 1024} Byte")
          }
        }
      }
    }
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def displayName(stack: ItemStack) = {
    val localizedName = parent.internalGetItemStackDisplayName(stack)
    Some(if (kiloBytes >= 1024) {
      localizedName + s" (${kiloBytes / 1024}MB)"
    }
    else {
      localizedName + s" (${kiloBytes}KB)"
    })
  }
}