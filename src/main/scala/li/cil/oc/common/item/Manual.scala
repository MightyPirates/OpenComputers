package li.cil.oc.common.item

import java.util

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.util.BlockPosition
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting
import net.minecraft.world.World

class Manual(val parent: Delegator) extends traits.Delegate {
  @SideOnly(Side.CLIENT)
  override def tooltipLines(stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean): Unit = {
    tooltip.add(EnumChatFormatting.DARK_GRAY.toString + "v" + OpenComputers.Version)
    super.tooltipLines(stack, player, tooltip, advanced)
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (world.isRemote) {
      if (player.isSneaking) {
        api.Manual.reset()
      }
      api.Manual.openFor(player)
    }
    super.onItemRightClick(stack, world, player)
  }

  override def onItemUse(stack: ItemStack, player: EntityPlayer, position: BlockPosition, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    val world = player.getEntityWorld
    api.Manual.pathFor(world, position.x, position.y, position.z) match {
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
