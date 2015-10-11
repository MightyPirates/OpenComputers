package li.cil.oc.common.item

import li.cil.oc.api
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumAction
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.world.World

class Acid(val parent: Delegator) extends traits.Delegate {
  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    player.setItemInUse(stack, getMaxItemUseDuration(stack))
    stack
  }

  override def getItemUseAction(stack: ItemStack): EnumAction = EnumAction.drink

  override def getMaxItemUseDuration(stack: ItemStack): Int = 32

  override def onEaten(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (!world.isRemote) {
      player.addPotionEffect(new PotionEffect(Potion.blindness.id, 200))
      player.addPotionEffect(new PotionEffect(Potion.poison.id, 100))
      player.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 600))
      player.addPotionEffect(new PotionEffect(Potion.confusion.id, 1200))
      player.addPotionEffect(new PotionEffect(Potion.field_76443_y.id, 2000))

      // Remove nanomachines if installed.
      api.Nanomachines.uninstallController(player)
    }
    stack.stackSize -= 1
    if (stack.stackSize > 0) stack
    else null
  }
}
