package li.cil.oc.common.item

import java.util

import li.cil.oc.Settings
import li.cil.oc.Settings.DebugCardAccess
import li.cil.oc.common.item.data.DebugCardData
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import li.cil.oc.server.command.string2text
import li.cil.oc.server.component.{DebugCard => CDebugCard}

class DebugCard(val parent: Delegator) extends traits.Delegate {
  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[String]): Unit = {
    super.tooltipExtended(stack, tooltip)
    val data = new DebugCardData(stack)
    data.access.foreach(access => tooltip.add(s"§8${access.player}§r"))
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (player.isSneaking) {
      val data = new DebugCardData(stack)
      val name = player.getCommandSenderName

      if (data.access.exists(_.player == name)) data.access = None
      else data.access =
        Some(CDebugCard.AccessContext(name, Settings.get.debugCardAccess match {
          case wl: DebugCardAccess.Whitelist => wl.nonce(name) match {
            case Some(n) => n
            case None =>
              player.addChatComponentMessage("§cYou are not whitelisted to use debug card")
              player.swingItem()
              return stack
          }

          case _ => ""
        }))

      data.save(stack)
      player.swingItem()
    }
    stack
  }
}
