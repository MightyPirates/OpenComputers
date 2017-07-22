package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumAction
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.world.World

class Chamelium(val parent: Delegator) extends traits.Delegate {
  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (Settings.get.chameliumEdible) {
      player.setItemInUse(stack, getMaxItemUseDuration(stack))
    }
    stack
  }

  override def getItemUseAction(stack: ItemStack): EnumAction = EnumAction.eat

  override def getMaxItemUseDuration(stack: ItemStack): Int = 32

  override def onEaten(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (!world.isRemote) {
      player.addPotionEffect(new PotionEffect(Potion.invisibility.id, 100, 0))
      player.addPotionEffect(new PotionEffect(Potion.blindness.id, 200, 0))
    }
    stack.stackSize -= 1
    if (stack.stackSize > 0) stack
    else null
  }
}
