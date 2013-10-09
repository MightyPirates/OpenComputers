package li.cil.oc.common.item

import java.util
import li.cil.oc.Config
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class Hdd(val parent: Delegator, val megaBytes: Int) extends Delegate {
  def unlocalizedName = "HardDiskDrive" + megaBytes + "m"

  override def addInformation(item: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) = {
    super.addInformation(item, player, tooltip, advanced)

    if (item.hasTagCompound) {
      val nbt = item.getTagCompound
      if (nbt.hasKey("oc.node")) {
        val nodeNbt = nbt.getCompoundTag("oc.node")
        if (nodeNbt.hasKey("label"))
          tooltip.add(nodeNbt.getString("label"))
        if (nodeNbt.hasKey("address"))
          tooltip.add(nodeNbt.getString("address"))
        if (advanced && nodeNbt.hasKey("fs")) {
          val fsNbt = nodeNbt.getCompoundTag("fs")
          if (fsNbt.hasKey("used")) {
            val used = fsNbt.getLong("used")
            tooltip.add("Disk usage: %d/%d Byte".format(used, megaBytes * 1024 * 1024))
          }
        }
      }
    }
  }

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":hdd" + megaBytes)
  }
}