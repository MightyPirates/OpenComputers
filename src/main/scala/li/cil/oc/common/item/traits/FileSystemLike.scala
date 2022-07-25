package li.cil.oc.common.item.traits

import java.util

import li.cil.oc.Localization
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import net.minecraft.client.util.ITooltipFlag
import li.cil.oc.common.item.data.DriveData
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Hand
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World

trait FileSystemLike extends Delegate {
  override protected def tooltipName = None

  def kiloBytes: Int

  override def tooltipLines(stack: ItemStack, world: World, tooltip: util.List[ITextComponent], flag: ITooltipFlag): Unit = {
    if (stack.hasTag) {
      val nbt = stack.getTag
      if (nbt.contains(Settings.namespace + "data")) {
        val data = nbt.getCompound(Settings.namespace + "data")
        if (data.contains(Settings.namespace + "fs.label")) {
          tooltip.add(new StringTextComponent(data.getString(Settings.namespace + "fs.label")))
        }
        if (flag.isAdvanced && data.contains("fs")) {
          val fsNbt = data.getCompound("fs")
          if (fsNbt.contains("capacity.used")) {
            val used = fsNbt.getLong("capacity.used")
            tooltip.add(new StringTextComponent(Localization.Tooltip.DiskUsage(used, kiloBytes * 1024)))
          }
        }
      }
      val data = new DriveData(stack)
      tooltip.add(new StringTextComponent(Localization.Tooltip.DiskMode(data.isUnmanaged)))
      tooltip.add(new StringTextComponent(Localization.Tooltip.DiskLock(data.lockInfo)))
    }
    super.tooltipLines(stack, world, tooltip, flag)
  }

  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    if (!player.isCrouching && (!stack.hasTag || !stack.getTag.contains(Settings.namespace + "lootFactory"))) {
      OpenComputers.openGui(player, GuiType.Drive.id, world, 0, 0, 0)
      player.swing(Hand.MAIN_HAND)
    }
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }
}
