package li.cil.oc.common.item

import java.util
import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class Disk(val parent: Delegator) extends Delegate {
  val unlocalizedName = "Disk"

  override def addInformation(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) = {
    if (stack.hasTagCompound && stack.getTagCompound.hasKey(Settings.namespace + "data")) {
      val nbt = stack.getTagCompound.getCompoundTag(Settings.namespace + "data")
      if (nbt.hasKey(Settings.namespace + "fs.label")) {
        tooltip.add(nbt.getString(Settings.namespace + "fs.label"))
      }
    }
    super.addInformation(stack, player, tooltip, advanced)
  }

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":disk")
  }
}
