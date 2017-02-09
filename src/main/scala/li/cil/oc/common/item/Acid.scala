package li.cil.oc.common.item

import javax.annotation.Nonnull

import li.cil.oc.api
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumAction
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.world.World

class Acid(val parent: Delegator) extends traits.Delegate {
  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ActionResult[ItemStack] = {
    player.setActiveHand(if (player.getHeldItemMainhand == stack) EnumHand.MAIN_HAND else EnumHand.OFF_HAND)
    ActionResult.newResult(EnumActionResult.SUCCESS, stack)
  }

  override def getItemUseAction(stack: ItemStack): EnumAction = EnumAction.DRINK

  override def getMaxItemUseDuration(stack: ItemStack): Int = 32

  override def onItemUseFinish(stack: ItemStack, world: World, entity: EntityLivingBase): ItemStack = {
    entity match {
      case player: EntityPlayer =>
        if (!world.isRemote) {
          player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("blindness"), 200))
          player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("poison"), 100))
          player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("slowness"), 600))
          player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("nausea"), 1200))
          player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("saturation"), 2000))

          // Remove nanomachines if installed.
          api.Nanomachines.uninstallController(player)
        }
        stack.shrink(1)
        if (stack.getCount > 0) stack
        else null
      case _ => stack
    }
  }
}
