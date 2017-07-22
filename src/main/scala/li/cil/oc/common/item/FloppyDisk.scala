package li.cil.oc.common.item

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.Color
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class FloppyDisk(val parent: Delegator) extends traits.Delegate with traits.FileSystemLike {
  // Necessary for anonymous subclasses used for loot disks.
  override def unlocalizedName = "FloppyDisk"

  val kiloBytes = Settings.get.floppySize

  val icons = Array.fill[Icon](16)(null)

  @SideOnly(Side.CLIENT)
  override def icon(stack: ItemStack, pass: Int) =
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "color"))
      Some(icons(stack.getTagCompound.getInteger(Settings.namespace + "color") max 0 min 15))
    else
      Some(icons(8))

  override def registerIcons(iconRegister: IconRegister) {
    val baseTextureName = Settings.resourceDomain + ":" + unlocalizedName + "_"
    Color.dyes.zipWithIndex.foreach {
      case (color, index) =>
        icons(index) = iconRegister.registerIcon(baseTextureName + color)
    }
  }

  override def doesSneakBypassUse(position: BlockPosition, player: EntityPlayer): Boolean = true
}
