package li.cil.oc.common.item

import java.util

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings
import li.cil.oc.util.Color
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class FloppyDisk(val parent: Delegator) extends Delegate {
  // Necessary for anonymous subclasses used for loot disks.
  override def unlocalizedName = "FloppyDisk"

  override protected def tooltipName = None

  val icons = Array.fill[Icon](16)(null)

  @SideOnly(Side.CLIENT)
  override def icon(stack: ItemStack, pass: Int) =
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "color"))
      Some(icons(stack.getTagCompound.getInteger(Settings.namespace + "color") max 0 min 15))
    else
      Some(icons(8))

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) = {
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "data")) {
      val nbt = stack.getTagCompound.getCompoundTag(Settings.namespace + "data")
      if (nbt.hasKey(Settings.namespace + "fs.label")) {
        tooltip.add(nbt.getString(Settings.namespace + "fs.label"))
      }
    }
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IconRegister) {
    val baseTextureName = Settings.resourceDomain + ":" + unlocalizedName + "_"
    Color.dyes.zipWithIndex.foreach {
      case (color, index) =>
        icons(index) = iconRegister.registerIcon(baseTextureName + color)
    }
  }
}
