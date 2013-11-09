package li.cil.oc.common.item

import java.util
import li.cil.oc.Config
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class HardDiskDrive(val parent: Delegator, val megaBytes: Int) extends Delegate {
  val unlocalizedName = "HardDiskDrive" + megaBytes + "m"

  override def addInformation(item: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) = {
    super.addInformation(item, player, tooltip, advanced)

    if (item.hasTagCompound) {
      val nbt = item.getTagCompound
      if (nbt.hasKey("oc.fs.label"))
        tooltip.add(nbt.getString("oc.fs.label"))
      if (nbt.hasKey("oc.node")) {
        val nodeNbt = nbt.getCompoundTag("oc.node")
        if (nodeNbt.hasKey("oc.node.address"))
          tooltip.add(nodeNbt.getString("oc.node.address"))
        if (advanced && nodeNbt.hasKey("fs")) {
          val fsNbt = nodeNbt.getCompoundTag("fs")
          if (fsNbt.hasKey("oc.capacity.used")) {
            val used = fsNbt.getLong("oc.capacity.used")
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