package li.cil.oc.common.item

import java.util
import li.cil.oc.Config
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class Disk(val parent: Delegator) extends Delegate {
  val unlocalizedName = "Disk"

  override def addInformation(item: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) = {
    super.addInformation(item, player, tooltip, advanced)

    if (item.hasTagCompound && item.getTagCompound.hasKey(Config.namespace + "data")) {
      val nbt = item.getTagCompound.getCompoundTag(Config.namespace + "data")
      if (nbt.hasKey(Config.namespace + "fs.label")) {
        tooltip.add(nbt.getString(Config.namespace + "fs.label"))
      }
    }
  }

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":disk")
  }
}
