package li.cil.oc.common.item.traits

import java.util

import li.cil.oc.Localization
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.common.item.data.DriveData
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.world.World

trait FileSystemLike extends Delegate {
  override protected def tooltipName = None

  def kiloBytes: Int

  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean): Unit = {
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
            tooltip.add(Localization.Tooltip.DiskUsage(used, kiloBytes * 1024))
          }
        }
      }
      val data = new DriveData(stack)
      tooltip.add(Localization.Tooltip.DiskMode(data.isUnmanaged))
      tooltip.add(Localization.Tooltip.DiskLock(data.lockInfo))
    }
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ActionResult[ItemStack] = {
    if (!player.isSneaking && (!stack.hasTagCompound || !stack.getTagCompound.hasKey(Settings.namespace + "lootFactory"))) {
      player.openGui(OpenComputers, GuiType.Drive.id, world, 0, 0, 0)
      player.swingArm(EnumHand.MAIN_HAND)
    }
    ActionResult.newResult(EnumActionResult.SUCCESS, stack)
  }
}
