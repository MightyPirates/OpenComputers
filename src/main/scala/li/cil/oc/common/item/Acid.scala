package li.cil.oc.common.item

import javax.annotation.Nonnull

import li.cil.oc.api
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemStack
import net.minecraft.item.UseAction
import net.minecraft.potion.Effect
import net.minecraft.potion.Effects
import net.minecraft.potion.EffectInstance
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Hand
import net.minecraft.world.World
import net.minecraftforge.common.extensions.IForgeItem

class Acid(props: Properties) extends Item(props) with IForgeItem with traits.SimpleItem {
  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    player.startUsingItem(if (player.getItemInHand(Hand.MAIN_HAND) == stack) Hand.MAIN_HAND else Hand.OFF_HAND)
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }

  override def getUseAnimation(stack: ItemStack): UseAction = UseAction.DRINK

  override def getUseDuration(stack: ItemStack): Int = 32

  override def finishUsingItem(stack: ItemStack, world: World, entity: LivingEntity): ItemStack = {
    entity match {
      case player: PlayerEntity =>
        if (!world.isClientSide) {
          player.addEffect(new EffectInstance(Effects.BLINDNESS, 200))
          player.addEffect(new EffectInstance(Effects.POISON, 100))
          player.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 600))
          player.addEffect(new EffectInstance(Effects.CONFUSION, 1200))
          player.addEffect(new EffectInstance(Effects.SATURATION, 2000))

          // Remove nanomachines if installed.
          api.Nanomachines.uninstallController(player)
        }
        stack.shrink(1)
        if (stack.getCount > 0) stack
        else ItemStack.EMPTY
      case _ => stack
    }
  }
}
