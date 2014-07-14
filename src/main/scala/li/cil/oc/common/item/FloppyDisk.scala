package li.cil.oc.common.item

import java.util

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.IIcon

class FloppyDisk(val parent: Delegator) extends Delegate {
  val unlocalizedName = "FloppyDisk"

  val icons = Array.fill[IIcon](16)(null)

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

  override def registerIcons(iconRegister: IIconRegister) {
    super.registerIcons(iconRegister)

    icons(0) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_black")
    icons(1) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_red")
    icons(2) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_green")
    icons(3) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_brown")
    icons(4) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_blue")
    icons(5) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_purple")
    icons(6) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_cyan")
    icons(7) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_lightGray")
    icons(8) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_gray")
    icons(9) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_pink")
    icons(10) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_lime")
    icons(11) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_yellow")
    icons(12) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_lightBlue")
    icons(13) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_magenta")
    icons(14) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_orange")
    icons(15) = iconRegister.registerIcon(Settings.resourceDomain + ":floppy_white")
  }
}
