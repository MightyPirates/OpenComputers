package li.cil.oc.common.item

import java.util

import li.cil.oc.Settings
import li.cil.oc.Settings.DebugCardAccess
import li.cil.oc.common.item.data.DebugCardData
import li.cil.oc.server.component.{DebugCard => CDebugCard}
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Hand
import net.minecraft.util.Util
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.World

class DebugCard(val parent: Delegator) extends traits.Delegate {
  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[ITextComponent]): Unit = {
    super.tooltipExtended(stack, tooltip)
    val data = new DebugCardData(stack)
    data.access.foreach(access => tooltip.add(new StringTextComponent(s"§8${access.player}§r")))
  }

  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    if (!world.isClientSide && player.isCrouching) {
      val data = new DebugCardData(stack)
      val name = player.getName

      if (data.access.exists(_.player == name)) data.access = None
      else data.access =
        Some(CDebugCard.AccessContext(name.getString, Settings.get.debugCardAccess match {
          case wl: DebugCardAccess.Whitelist => wl.nonce(name.getString) match {
            case Some(n) => n
            case None =>
              player.sendMessage(new StringTextComponent("§cYou are not whitelisted to use debug card"), Util.NIL_UUID)
              player.swing(Hand.MAIN_HAND)
              return new ActionResult[ItemStack](ActionResultType.FAIL, stack)
          }

          case _ => ""
        }))

      data.saveData(stack)
      player.swing(Hand.MAIN_HAND)
    }
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }
}
