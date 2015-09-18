package li.cil.oc.common.item

import li.cil.oc.api
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumAction
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class Nanomachines(val parent: Delegator) extends traits.Delegate {
  override def rarity(stack: ItemStack): EnumRarity = EnumRarity.UNCOMMON

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    player.setItemInUse(stack, getMaxItemUseDuration(stack))
    stack
  }

  override def getItemUseAction(stack: ItemStack): EnumAction = EnumAction.EAT

  override def getMaxItemUseDuration(stack: ItemStack): Int = 32

  override def onItemUseFinish(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    if (!world.isRemote) {
      // Reconfigure if already installed.
      api.Nanomachines.installController(player).reconfigure()
    }
    stack.stackSize -= 1
    if (stack.stackSize > 0) stack
    else null
  }
}
