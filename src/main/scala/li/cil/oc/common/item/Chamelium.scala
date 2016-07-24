package li.cil.oc.common.item

import li.cil.oc.Settings
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

class Chamelium(val parent: Delegator) extends traits.Delegate {
  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ActionResult[ItemStack] = {
    if (Settings.get.chameliumEdible) {
      player.setActiveHand(if (player.getHeldItemMainhand == stack) EnumHand.MAIN_HAND else EnumHand.OFF_HAND)
    }
    ActionResult.newResult(EnumActionResult.SUCCESS, stack)
  }

  override def getItemUseAction(stack: ItemStack): EnumAction = EnumAction.EAT

  override def getMaxItemUseDuration(stack: ItemStack): Int = 32

  override def onItemUseFinish(stack: ItemStack, world: World, player: EntityLivingBase): ItemStack = {
    if (!world.isRemote) {
      player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("invisibility"), 100, 0))
      player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("blindness"), 200, 0))
    }
    stack.stackSize -= 1
    if (stack.stackSize > 0) stack
    else null
  }
}
