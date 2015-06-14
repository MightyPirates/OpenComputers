package li.cil.oc.common.item

import java.util

import li.cil.oc.common.item.data.DebugCardData
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class DebugCard(val parent: Delegator) extends traits.Delegate {
  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[String]): Unit = {
    super.tooltipExtended(stack, tooltip)
    val data = new DebugCardData(stack)
    data.player.foreach(name => tooltip.add(s"§8$name§r"))
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (player.isSneaking) {
      val data = new DebugCardData(stack)
      if (data.player.contains(player.getCommandSenderName)) data.player = None
      else data.player = Option(player.getCommandSenderName)
      data.save(stack)
      player.swingItem()
    }
    stack
  }
}
