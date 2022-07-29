package li.cil.oc.common.item

import li.cil.oc.CreativeTab
import li.cil.oc.Settings
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraftforge.common.extensions.IForgeItem

class HardDiskDrive(val tier: Int, props: Properties = new Properties().tab(CreativeTab)) extends Item(props) with IForgeItem with traits.SimpleItem with traits.ItemTier with traits.FileSystemLike {
  @Deprecated
  override def getDescriptionId = super.getDescriptionId + tier

  val kiloBytes: Int = Settings.get.hddSizes(tier)
  val platterCount: Int = Settings.get.hddPlatterCounts(tier)

  override def getName(stack: ItemStack): ITextComponent = {
    val localizedName = super.getName(stack).copy()
    if (kiloBytes >= 1024) {
      localizedName.append(s" (${kiloBytes / 1024}MB)")
    }
    else {
      localizedName.append(s" (${kiloBytes}KB)")
    }
    localizedName
  }
}