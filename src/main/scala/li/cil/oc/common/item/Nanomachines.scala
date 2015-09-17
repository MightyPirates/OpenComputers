package li.cil.oc.common.item

import li.cil.oc.api
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumAction
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class Nanomachines(val parent: Delegator) extends traits.Delegate {
  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (!api.Nanomachines.hasController(player)) {
      player.setItemInUse(stack, getMaxItemUseDuration(stack))
    }
    stack
  }

  override def getItemUseAction(stack: ItemStack): EnumAction = EnumAction.eat

  override def getMaxItemUseDuration(stack: ItemStack): Int = 32

  override def onEaten(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (!world.isRemote && !api.Nanomachines.hasController(player)) {
      api.Nanomachines.installController(player).reconfigure()

      stack.stackSize -= 1
    }
    if (stack.stackSize > 0) stack
    else null
  }
}
