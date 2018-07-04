package li.cil.oc.common.item

import java.util

import com.mojang.realmsclient.gui.ChatFormatting
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.util.BlockPosition
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class Manual(val parent: Delegator) extends traits.Delegate {
  @SideOnly(Side.CLIENT)
  override def tooltipLines(stack: ItemStack, world: World, tooltip: util.List[String], flag: ITooltipFlag): Unit = {
    tooltip.add(ChatFormatting.DARK_GRAY.toString + "v" + OpenComputers.Version)
    super.tooltipLines(stack, world, tooltip, flag)
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ActionResult[ItemStack] = {
    if (world.isRemote) {
      if (player.isSneaking) {
        api.Manual.reset()
      }
      api.Manual.openFor(player)
    }
    ActionResult.newResult(EnumActionResult.SUCCESS, stack)
  }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    val world = player.getEntityWorld
    api.Manual.pathFor(world, position.toBlockPos) match {
      case path: String =>
        if (world.isRemote) {
          api.Manual.openFor(player)
          api.Manual.reset()
          api.Manual.navigate(path)
        }
        true
      case _ => super.onItemUse(stack, player, position, side, hitX, hitY, hitZ)
    }
  }
}
