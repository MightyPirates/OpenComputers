package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.UseAction
import net.minecraft.item.ItemStack
import net.minecraft.potion.Effect
import net.minecraft.potion.Effects
import net.minecraft.potion.EffectInstance
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Hand
import net.minecraft.world.World

class Chamelium(val parent: Delegator) extends traits.Delegate {
  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    if (Settings.get.chameliumEdible) {
      player.startUsingItem(if (player.getItemInHand(Hand.MAIN_HAND) == stack) Hand.MAIN_HAND else Hand.OFF_HAND)
    }
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }

  override def getUseAnimation(stack: ItemStack): UseAction = UseAction.EAT

  override def getMaxItemUseDuration(stack: ItemStack): Int = 32

  override def finishUsingItem(stack: ItemStack, world: World, player: LivingEntity): ItemStack = {
    if (!world.isClientSide) {
      player.addEffect(new EffectInstance(Effects.INVISIBILITY, 100, 0))
      player.addEffect(new EffectInstance(Effects.BLINDNESS, 200, 0))
    }
    stack.shrink(1)
    if (stack.getCount > 0) stack
    else ItemStack.EMPTY
  }
}
